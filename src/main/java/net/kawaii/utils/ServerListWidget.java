package net.kawaii.utils;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kawaii.Client;
import net.kawaii.screens.EthanolServersScreen;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.screen.world.WorldIcon;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.network.ServerInfo;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.ethanol.ethanolapi.server.listener.EthanolServer;

public class ServerListWidget extends AlwaysSelectedEntryListWidget<ServerListWidget.Entry> {
   static final Logger LOGGER = LoggerFactory.getLogger(ServerListWidget.class);
   static final ThreadPoolExecutor SERVER_PINGER_THREAD_POOL;
   static final Identifier UNKNOWN_SERVER_TEXTURE;
   static final Identifier SERVER_SELECTION_TEXTURE;
   static final Identifier ICONS_TEXTURE;
   static final Text LAN_SCANNING_TEXT;
   static final Text CANNOT_RESOLVE_TEXT;
   static final Text CANNOT_CONNECT_TEXT;
   static final Text INCOMPATIBLE_TEXT;
   static final Text NO_CONNECTION_TEXT;
   static final Text PINGING_TEXT;
   static final Identifier INCOMPATIBLE_TEXTURE = Identifier.ofVanilla("server_list/incompatible");
   static final Identifier UNREACHABLE_TEXTURE = Identifier.ofVanilla("server_list/unreachable");
   static final Identifier PING_1_TEXTURE = Identifier.ofVanilla("server_list/ping_1");
   static final Identifier PING_2_TEXTURE = Identifier.ofVanilla("server_list/ping_2");
   static final Identifier PING_3_TEXTURE = Identifier.ofVanilla("server_list/ping_3");
   static final Identifier PING_4_TEXTURE = Identifier.ofVanilla("server_list/ping_4");
   static final Identifier PING_5_TEXTURE = Identifier.ofVanilla("server_list/ping_5");
   static final Identifier PINGING_1_TEXTURE = Identifier.ofVanilla("server_list/pinging_1");
   static final Identifier PINGING_2_TEXTURE = Identifier.ofVanilla("server_list/pinging_2");
   static final Identifier PINGING_3_TEXTURE = Identifier.ofVanilla("server_list/pinging_3");
   static final Identifier PINGING_4_TEXTURE = Identifier.ofVanilla("server_list/pinging_4");
   static final Identifier PINGING_5_TEXTURE = Identifier.ofVanilla("server_list/pinging_5");
   static final Identifier JOIN_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("server_list/join_highlighted");
   static final Identifier JOIN_TEXTURE = Identifier.ofVanilla("server_list/join");
   static final Identifier MOVE_UP_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("server_list/move_up_highlighted");
   static final Identifier MOVE_UP_TEXTURE = Identifier.ofVanilla("server_list/move_up");
   static final Identifier MOVE_DOWN_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("server_list/move_down_highlighted");
   static final Identifier MOVE_DOWN_TEXTURE = Identifier.ofVanilla("server_list/move_down");
   private final EthanolServersScreen screen;
   private static final List<ServerEntry> SERVERS = new CopyOnWriteArrayList<>();
   private static final List<String> hideServersAddress = new CopyOnWriteArrayList<>();
   private ForkJoinPool serverListPingPool;

   public ServerListWidget(EthanolServersScreen multiplayerScreen, MinecraftClient minecraftClient, int i, int j, int k, int l, int m) {
      super(minecraftClient, i, j, k, l, m);
      this.screen = multiplayerScreen;
      this.serverListPingPool = new ForkJoinPool(1);
   }

   public List<ServerEntry> getServers() {
      synchronized(SERVERS) {
         return SERVERS;
      }
   }

   @Override
   public int addEntry(Entry entry) {
      return super.addEntry(entry);
   }

   private void updateEntries() {
      synchronized(SERVERS) {
         this.replaceEntries(Collections.unmodifiableList(SERVERS.stream().filter((s) -> !hideServersAddress.contains(s.server.address)).collect(Collectors.toList())));
      }
   }

   public void setSelected(@Nullable Entry entry) {
      super.setSelected(entry);
      this.screen.updateButtonActivationStates();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      Entry entry = (Entry)this.getSelectedOrNull();
      return entry != null ? entry.keyPressed(keyCode, scanCode, modifiers) : super.keyPressed(keyCode, scanCode, modifiers);
   }

   public void setServers(ServerList servers) {
      synchronized(SERVERS) {
         SERVERS.clear();
         this.screen.getServerListPinger().cancel();
         this.serverListPingPool.shutdownNow();
         this.serverListPingPool = new ForkJoinPool(100);

         for(int i = 0; i < servers.size(); ++i) {
            SERVERS.add(new ServerEntry(this.screen, servers.get(i)));
         }

         this.updateEntries();
         (new ArrayList<>(SERVERS)).parallelStream().forEach((serverEntry) -> {
            this.serverListPingPool.submit(() -> {
               try {
                  this.screen.getServerListPinger().add(serverEntry.server, () -> {
                     this.serverListPingPool.submit(this::updateEntries);
                  }, () -> {});
               } catch (UnknownHostException var3) {
                  serverEntry.server.ping = -1L;
                  serverEntry.server.label = CANNOT_RESOLVE_TEXT;
               } catch (Exception var4) {
                  serverEntry.server.ping = -1L;
                  serverEntry.server.label = CANNOT_CONNECT_TEXT;
               }

            });
         });
      }
   }

   public int getRowWidth() {
      return super.getRowWidth() + 85;
   }

   public boolean isFocused() {
      return this.screen.getFocused() == this;
   }

   static {
      SERVER_PINGER_THREAD_POOL = new ScheduledThreadPoolExecutor(25, (new ThreadFactoryBuilder()).setNameFormat("Server Pinger #%d").setDaemon(true).setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER)).build());
      UNKNOWN_SERVER_TEXTURE = Identifier.of("textures/misc/unknown_server.png");
      SERVER_SELECTION_TEXTURE = Identifier.of("textures/gui/server_selection.png");
      ICONS_TEXTURE = Identifier.of("textures/gui/icons.png");
      LAN_SCANNING_TEXT = Text.translatable("lanServer.scanning");
      CANNOT_RESOLVE_TEXT = Text.translatable("multiplayer.status.cannot_resolve").formatted(Formatting.DARK_RED);
      CANNOT_CONNECT_TEXT = Text.translatable("multiplayer.status.cannot_connect").formatted(Formatting.DARK_RED);
      INCOMPATIBLE_TEXT = Text.translatable("multiplayer.status.incompatible");
      NO_CONNECTION_TEXT = Text.translatable("multiplayer.status.no_connection");
      PINGING_TEXT = Text.translatable("multiplayer.status.pinging");
   }

   @Environment(EnvType.CLIENT)
   public abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
   }

   @Environment(EnvType.CLIENT)
   public class ServerEntry extends Entry {
      private final EthanolServersScreen screen;
      private final ServerInfo server;
      private final WorldIcon icon;
      private long time;
      byte[] favicon;
      private Text statusTooltipText;
      @Nullable
      private Identifier statusIconTexture;
      @Nullable
      private List<Text> playerListSummary;

      public ServerEntry(EthanolServersScreen multiplayerScreen, ServerInfo serverInfo) {
         this.screen = multiplayerScreen;
         this.server = serverInfo;
         this.icon = WorldIcon.forServer(Client.mc.getTextureManager(), server.address);
         this.update();
      }

      public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickProgress) {
         if (this.server.getStatus() == ServerInfo.Status.INITIAL) {
            this.server.setStatus(ServerInfo.Status.PINGING);
            this.server.label = ScreenTexts.EMPTY;
            this.server.playerCountLabel = ScreenTexts.EMPTY;
            ServerListWidget.SERVER_PINGER_THREAD_POOL.submit(() -> {
               try {
                  this.screen.getServerListPinger().add(this.server, () -> {}, () -> {
                     this.server.setStatus(this.server.protocolVersion == SharedConstants.getGameVersion().protocolVersion() ? ServerInfo.Status.SUCCESSFUL : ServerInfo.Status.INCOMPATIBLE);
                     Client.mc.execute(this::update);
                  });
               } catch (UnknownHostException var2) {
                  this.server.setStatus(ServerInfo.Status.UNREACHABLE);
                  this.server.label = ServerListWidget.CANNOT_RESOLVE_TEXT;
                  Client.mc.execute(this::update);
               } catch (Exception var3) {
                  this.server.setStatus(ServerInfo.Status.UNREACHABLE);
                  this.server.label = ServerListWidget.CANNOT_CONNECT_TEXT;
                  Client.mc.execute(this::update);
               }

            });
         }

         context.drawTextWithShadow(Client.mc.textRenderer, this.server.name, x + 32 + 3, y + 1, -1);
         List<OrderedText> list = Client.mc.textRenderer.wrapLines(this.server.label, entryWidth - 32 - 2);

         for(int i = 0; i < Math.min(list.size(), 2); ++i) {
            TextRenderer var10001 = Client.mc.textRenderer;
            OrderedText var10002 = (OrderedText)list.get(i);
            int var10003 = x + 32 + 3;
            int var10004 = y + 12;
            Objects.requireNonNull(Client.mc.textRenderer);
            context.drawTextWithShadow(var10001, var10002, var10003, var10004 + 9 * i, -8355712);
         }

         this.draw(context, x, y, this.icon.getTextureId());
         if (this.server.getStatus() == ServerInfo.Status.PINGING) {
            int i = (int)(Util.getMeasuringTimeMs() / 100L + (long)(index * 2) & 7L);
            if (i > 4) {
               i = 8 - i;
            }

            Identifier var21;
            switch (i) {
               case 1 -> var21 = ServerListWidget.PINGING_2_TEXTURE;
               case 2 -> var21 = ServerListWidget.PINGING_3_TEXTURE;
               case 3 -> var21 = ServerListWidget.PINGING_4_TEXTURE;
               case 4 -> var21 = ServerListWidget.PINGING_5_TEXTURE;
               default -> var21 = ServerListWidget.PINGING_1_TEXTURE;
            }

            this.statusIconTexture = var21;
         }

         int i = x + entryWidth - 10 - 5;
         if (this.statusIconTexture != null) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, this.statusIconTexture, i, y, 10, 8);
         }

         byte[] bs = this.server.getFavicon();
         this.server.setFavicon((byte[])null);
         if (!Arrays.equals(bs, this.favicon)) {
            if (this.uploadFavicon(bs)) {
               this.favicon = bs;
            } else {
               this.server.setFavicon((byte[])null);
            }
         }

         Text text = (Text)(this.server.getStatus() == ServerInfo.Status.INCOMPATIBLE ? this.server.version.copy().formatted(Formatting.RED) : this.server.playerCountLabel);
         int j = Client.mc.textRenderer.getWidth(text);
         int k = i - j - 5;
         context.drawTextWithShadow(Client.mc.textRenderer, text, k, y + 1, -8355712);
         if (this.statusTooltipText != null && mouseX >= i && mouseX <= i + 10 && mouseY >= y && mouseY <= y + 8) {
            context.drawTooltip(Text.of(this.statusTooltipText), mouseX, mouseY);
         }
         else if (this.playerListSummary != null && mouseX >= k && mouseX <= k + j && mouseY >= y) {
            int var22 = y - 1;
            Objects.requireNonNull(Client.mc.textRenderer);
            if (mouseY <= var22 + 9) {
               context.drawTooltip(Lists.transform(this.playerListSummary, Text::asOrderedText), mouseX, mouseY);
            }
         }

         if ((Boolean)Client.mc.options.getTouchscreen().getValue() || hovered) {
            context.fill(x, y, x + 32, y + 32, -1601138544);
            int l = mouseX - x;
            int m = mouseY - y;
            if (this.canConnect()) {
               if (l < 32 && l > 16) {
                  context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ServerListWidget.JOIN_HIGHLIGHTED_TEXTURE, x, y, 32, 32);
               } else {
                  context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ServerListWidget.JOIN_TEXTURE, x, y, 32, 32);
               }
            }

            if (index > 0) {
               if (l < 16 && m < 16) {
                  context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ServerListWidget.MOVE_UP_HIGHLIGHTED_TEXTURE, x, y, 32, 32);
               } else {
                  context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ServerListWidget.MOVE_UP_TEXTURE, x, y, 32, 32);
               }
            }

            if (index < this.screen.getServerList().size() - 1) {
               if (l < 16 && m > 16) {
                  context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ServerListWidget.MOVE_DOWN_HIGHLIGHTED_TEXTURE, x, y, 32, 32);
               } else {
                  context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ServerListWidget.MOVE_DOWN_TEXTURE, x, y, 32, 32);
               }
            }
         }
      }

      private void update() {
         this.playerListSummary = null;
         switch (this.server.getStatus()) {
            case INITIAL:
            case PINGING:
               this.statusIconTexture = ServerListWidget.PING_1_TEXTURE;
               this.statusTooltipText = ServerListWidget.PINGING_TEXT;
               break;
            case INCOMPATIBLE:
               this.statusIconTexture = ServerListWidget.INCOMPATIBLE_TEXTURE;
               this.statusTooltipText = ServerListWidget.INCOMPATIBLE_TEXT;
               this.playerListSummary = this.server.playerListSummary;
               break;
            case UNREACHABLE:
               this.statusIconTexture = ServerListWidget.UNREACHABLE_TEXTURE;
               this.statusTooltipText = ServerListWidget.NO_CONNECTION_TEXT;
               break;
            case SUCCESSFUL:
               if (this.server.ping < 150L) {
                  this.statusIconTexture = ServerListWidget.PING_5_TEXTURE;
               } else if (this.server.ping < 300L) {
                  this.statusIconTexture = ServerListWidget.PING_4_TEXTURE;
               } else if (this.server.ping < 600L) {
                  this.statusIconTexture = ServerListWidget.PING_3_TEXTURE;
               } else if (this.server.ping < 1000L) {
                  this.statusIconTexture = ServerListWidget.PING_2_TEXTURE;
               } else {
                  this.statusIconTexture = ServerListWidget.PING_1_TEXTURE;
               }

               this.statusTooltipText = Text.translatable("multiplayer.status.ping", new Object[]{this.server.ping});
               this.playerListSummary = this.server.playerListSummary;
         }
      }

      private boolean uploadFavicon(@Nullable byte[] bytes) {
         if (bytes == null) {
            this.icon.destroy();
         } else {
            try {
               this.icon.load(NativeImage.read(bytes));
            } catch (Throwable throwable) {
               Client.LOGGER.error("Invalid icon for server {} ({})", new Object[]{this.server.name, this.server.address, throwable});
               return false;
            }
         }

         return true;
      }

      public void setStatusTooltipText(String statusTooltipText) {
         this.statusTooltipText = Text.literal(statusTooltipText);
      }

      protected void draw(DrawContext context, int x, int y, Identifier textureId) {
         context.drawTexture(RenderPipelines.GUI_TEXTURED, textureId, x, y, 0.0F, 0.0F, 32, 32, 32, 32);
      }

      private boolean canConnect() {
         return true;
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         double d = mouseX - (double) ServerListWidget.this.getRowLeft();
         if (d <= 32.0 && d < 32.0 && d > 16.0 && this.canConnect()) {
            this.screen.select(this);
            this.screen.connect();
            return true;
         } else {
            this.screen.select(this);
            if (Util.getMeasuringTimeMs() - this.time < 250L) {
               this.screen.connect();
            }

            this.time = Util.getMeasuringTimeMs();
            return false;
         }
      }

      public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         if (Screen.isCopy(keyCode)) {
            Client.mc.keyboard.setClipboard(this.server.address);
            return true;
         } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
         }
      }

      public ServerInfo getServer() {
         return this.server;
      }

      public Text getNarration() {
         return Text.translatable("narrator.select", this.server.name);
      }
   }
}