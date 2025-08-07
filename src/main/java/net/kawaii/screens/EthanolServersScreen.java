package net.kawaii.screens;

import net.kawaii.Client;
import net.kawaii.utils.CustomServerListPinger;
import net.kawaii.utils.EthanolSystem;
import net.kawaii.utils.ServerList;
import net.kawaii.utils.ServerListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.*;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.input.KeyCodes;
import net.minecraft.client.network.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import rocks.ethanol.ethanolapi.EthanolAPI;
import rocks.ethanol.ethanolapi.server.listener.EthanolServer;
import rocks.ethanol.ethanolapi.server.listener.EthanolServerListener;
import rocks.ethanol.ethanolapi.server.listener.exceptions.EthanolServerListenerConnectException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EthanolServersScreen extends Screen {
    public static EthanolServersScreen INSTANCE;

    private ServerListWidget serverListWidget;
    private ButtonWidget joinButton;
    private ButtonWidget consoleButton;
    private CustomServerListPinger multiplayerServerListPinger = new CustomServerListPinger();
    private EthanolServerListener listener = Client.EthanolListener;
    Screen parent = null;
    List<String> servers = List.of("45.179.80.174:25567", "play.minehut.com", "play.minefort.com");
    private boolean initialized = false;

    private final ServerList serverList = new ServerList();

    public static EthanolServersScreen instance(Screen parent){
        if (INSTANCE != null){
            INSTANCE.parent = parent;
            return INSTANCE;
        }
        return new EthanolServersScreen(parent);
    }

    public EthanolServersScreen(Screen parent) {
        super(Text.of("Ethanol"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (this.initialized) {
            this.serverListWidget.setDimensionsAndPosition(this.width, this.height - 64 - 32, 0, 32);
        } else {
            this.initialized = true;

            if (listener == null) {
                listener = EthanolAPI.createServerListener(EthanolSystem.apiKey);
                Client.EthanolListener = listener;
                try {
                    listener.run();
                } catch (IOException | EthanolServerListenerConnectException ignored) {
                }
            }

            this.serverListWidget = new ServerListWidget(this, this.client, this.width, this.height, 32, 36, 0);
            this.serverListWidget.setDimensionsAndPosition(this.width, this.height - 64 - 32, 0, 32);

            for (EthanolServer server : listener.getServers()){
                String substring = server.getAddress().toString().substring(1);
                serverList.add(new ServerInfo(substring, substring, ServerInfo.ServerType.OTHER));
            }

            for (String s : servers){
                serverList.add(new ServerInfo(s, s, ServerInfo.ServerType.OTHER));
            }

            this.serverListWidget.setServers(this.serverList);
            AllowedAddressResolver allowedAddressResolver = AllowedAddressResolver.DEFAULT;
        }

        this.addDrawableChild(this.serverListWidget);

        this.joinButton = (ButtonWidget)this.addDrawableChild(ButtonWidget.builder(Text.translatable("selectServer.select"), (button) -> this.connect()).width(100).build());
        this.consoleButton = (ButtonWidget)this.addDrawableChild(ButtonWidget.builder(Text.literal("console"), (button) -> {
            if (serverListWidget.getSelectedOrNull() != null){
                this.client.setScreen(new ServerConsoleScreen(getSelectedEthanolServer()));
            }
        }).width(100).build());
        ButtonWidget buttonWidget4 = (ButtonWidget)this.addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, (button) -> this.close()).width(74).build());
        DirectionalLayoutWidget directionalLayoutWidget = DirectionalLayoutWidget.vertical();
        AxisGridWidget axisGridWidget = (AxisGridWidget)directionalLayoutWidget.add(new AxisGridWidget(308, 20, AxisGridWidget.DisplayAxis.HORIZONTAL));
        axisGridWidget.add(this.joinButton);
        axisGridWidget.add(this.consoleButton);
        directionalLayoutWidget.add(EmptyWidget.ofHeight(4));
        AxisGridWidget axisGridWidget2 = (AxisGridWidget)directionalLayoutWidget.add(new AxisGridWidget(308, 20, AxisGridWidget.DisplayAxis.HORIZONTAL));
        axisGridWidget2.add(buttonWidget4);
        directionalLayoutWidget.refreshPositions();
        SimplePositioningWidget.setPos(directionalLayoutWidget, 0, this.height - 64, this.width, 64);

        this.updateButtonActivationStates();
    }

    @Override
    public void close() {
        Client.mc.setScreen(this.parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (this.serverListWidget != null && this.serverListWidget.getSelectedOrNull() != null) {
            if (KeyCodes.isToggle(keyCode)) {
                this.connect();
                return true;
            } else {
                return this.serverListWidget.keyPressed(keyCode, scanCode, modifiers);
            }
        } else {
            return false;
        }
    }

    public ServerList getServerList() {
        return serverList;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, -1);
    }

    public void connect() {
        ServerListWidget.Entry entry = (ServerListWidget.Entry) this.serverListWidget.getSelectedOrNull();
        if (entry instanceof ServerListWidget.ServerEntry) {
            this.connect(((ServerListWidget.ServerEntry)entry).getServer());
        }
    }

    private void connect(ServerInfo entry) {
        ConnectScreen.connect(this, this.client, ServerAddress.parse(entry.address), entry, false, (CookieStorage)null);
    }

    public void select(ServerListWidget.Entry entry) {
        this.serverListWidget.setSelected(entry);
        this.updateButtonActivationStates();
    }

    public EthanolServer getSelectedEthanolServer() {
        ServerListWidget.Entry entry = (ServerListWidget.Entry) this.serverListWidget.getSelectedOrNull();
        if (entry instanceof ServerListWidget.ServerEntry) {
            ServerInfo info = ((ServerListWidget.ServerEntry)entry).getServer();
            for (EthanolServer server : listener.getServers()){
                if (server.getAddress().toString().substring(1).equals(info.address)) return server;
            }
        }
        return null;
    }

    public void updateButtonActivationStates() {
        this.joinButton.active = false;
        this.consoleButton.active = false;
        ServerListWidget.Entry entry = (ServerListWidget.Entry) this.serverListWidget.getSelectedOrNull();
        if (entry != null) {
            this.joinButton.active = true;
            this.consoleButton.active = true;
        }
    }

    public MultiplayerServerListPinger getServerListPinger() {
        return multiplayerServerListPinger;
    }

    @Override
    public void tick() {
        super.tick();
        this.multiplayerServerListPinger.tick();
    }
}
