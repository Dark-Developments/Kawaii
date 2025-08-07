package net.kawaii.screens;

import com.google.common.collect.EvictingQueue;
import com.mojang.brigadier.suggestion.Suggestions;
import net.kawaii.Client;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import rocks.ethanol.ethanolapi.EthanolAPI;
import rocks.ethanol.ethanolapi.server.connector.EthanolServerConnector;
import rocks.ethanol.ethanolapi.server.listener.EthanolServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class ServerConsoleScreen extends Screen {

	public static final Text ENTER_COMMAND = Text.literal("Command");

	private final Queue<String> logQueue = EvictingQueue.create(256);
	private final List<String> messages = new ArrayList<>();
	private int messagePos = -1;
	private double scrollAmount;
	private boolean scrollMax = true;
	private final List<String> consoleOutput = new ArrayList<>();
	private final EthanolServer server;
	private EthanolServerConnector connector;

	public Output output;
	public TextFieldWidget input;

	public ServerConsoleScreen(EthanolServer server) {
		super(Text.empty());
		this.server = server;
		this.connector = EthanolAPI.connect(this.server.getAuthentication());

		CompletableFuture<EthanolServerConnector> future = connector.startAsync();

		appendConsoleOutput("Connecting to server " + server.getAddress().toString().substring(1));

		future.thenAccept(conn -> {
			appendConsoleOutput("Connected to the server!");

			// Listen for messages from the server
			conn.listen(message -> appendConsoleOutput("Server: " + message));

		}).exceptionally(ex -> {
			appendConsoleOutput("Failed to connect: " + ex.getMessage());
			return null;
		});

		// Add a shutdown hook to close the connector gracefully
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (!connector.isClosed()) {
					connector.close();
					appendConsoleOutput("Connector closed!");
				}
			} catch (Exception e) {
				appendConsoleOutput("Error closing connector: " + e.getMessage());
			}
		}));
	}

	@Override
	protected void init() {
		super.init();
		messagePos = messages.size();

		output = new Output() {
			@Override
			protected void appendClickableNarrations(NarrationMessageBuilder builder) {

			}
		};
		addDrawableChild(output);

		input = new TextFieldWidget(this.textRenderer, 20, this.height - 30, this.width - 40, 20, Text.empty());
		input.setPlaceholder(ENTER_COMMAND);
		addDrawableChild(input);
	}

	private void appendConsoleOutput(String message) {
		synchronized (consoleOutput) {
			consoleOutput.add(message);
		}
		if (output != null) {
			output.addLine(message);
		}
	}

	@Override
	public void renderBackground(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(drawContext, mouseX, mouseY, partialTicks);
	}


	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// Let the input field handle the key first
		if (input.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}

		// ESC to unfocus
		if (keyCode == GLFW.GLFW_KEY_ESCAPE && input.isFocused()) {
			setFocused(null);
			return true;
		}

		// ENTER to send message
		if (keyCode == GLFW.GLFW_KEY_ENTER) {
			if (!input.isFocused()) {
				setFocused(input);
				input.setEditable(true);
				return true;
			} else if (!input.getText().isEmpty()) {
				String newText = input.getText();
				input.setText("");

				appendConsoleOutput("You: " + newText);

				if (connector != null && !newText.trim().isEmpty()) {
					try {
						connector.writeLine(newText);
					} catch (Exception e) {
						appendConsoleOutput("Error sending message: " + e.getMessage());
					}
				}

				messages.add(newText);
				messagePos = messages.size();

				return true;
			}
		}

		// History navigation when input is not focused
		if (!input.isFocused()) {
			if (keyCode == GLFW.GLFW_KEY_LEFT) {
				moveMessagePos(-1);
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
				moveMessagePos(1);
				return true;
			}
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (input.charTyped(chr, modifiers)) {
			return true;
		}
		return super.charTyped(chr, modifiers);
	}

	public void moveMessagePos(int delta) {
		int newPos = messagePos + delta;
		if (newPos < 0 || newPos >= messages.size()) {
			return;
		}
		messagePos = newPos;
		input.setCursor(newPos, false);
		input.setText(messages.get(messagePos));
	}

	@Override
	public void close() {
		scrollAmount = output.getScrollAmount();
		scrollMax = scrollAmount == output.getMaxScrollAmount();
		super.close();
	}

	public abstract class Output extends ContainerWidget {
		private final List<AbstractTextWidget> widgets = new ArrayList<>();
		private int widgetsHeight = 0;

		private double scrollAmount = 0;

		public Output() {
			super(20, 10, ServerConsoleScreen.this.width - 40, ServerConsoleScreen.this.height - 50, Text.empty());

			// Initialize widgets from consoleOutput
			synchronized (consoleOutput) {
				for (String line : consoleOutput) {
					addLineInternal(line);
				}
			}
			setScrollAmount(getMaxScrollAmount());
		}

		private void addLineInternal(String line) {
			boolean max = scrollAmount == getMaxScrollAmount();
			MultilineTextWidget widget = new MultilineTextWidget(Text.literal(line), Client.mc.textRenderer);
			widget.setPosition(getX() + 3, getY() + widgetsHeight + 3);
			widget.setMaxWidth(getWidth() + 17 - getX());
			widgets.add(widget);
			widgetsHeight += widget.getHeight();
			if (max) {
				setScrollAmount(getMaxScrollAmount());
			}
		}

		public void addLine(String line) {
			boolean atBottom = scrollAmount >= getMaxScrollAmount() - 1; // small tolerance
			addLineInternal(line);
			if (atBottom) {
				setScrollAmount(getMaxScrollAmount());
			}
		}

		@Override
		protected int getContentsHeightWithPadding() {
			return widgetsHeight + 5;
		}

		@Override
		protected double getDeltaYPerScroll() {
			return 10.0;
		}

		public double getScrollAmount() {
			return scrollAmount;
		}

		public double getMaxScrollAmount() {
			return Math.max(0, getContentsHeightWithPadding() - getHeight());
		}

		public void setScrollAmount(double scroll) {
			scrollAmount = Math.max(0, Math.min(scroll, getMaxScrollAmount()));
		}

		@Override
		protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			// Background
			context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF000000);

			// Scissor for clipping content
			context.enableScissor(getX(), getY(), getWidth(), getHeight());

			// Translate by scroll amount
			context.getMatrices().pushMatrix();
			context.getMatrices().translate(0.0F, (float) -scrollAmount);

			// Render all text lines
			for (AbstractTextWidget widget : widgets) {
				widget.render(context, mouseX, mouseY, deltaTicks);
			}

			context.getMatrices().popMatrix();

			context.disableScissor();

			context.drawBorder(getX(), getY(), getWidth(), getHeight(), isFocused() ? 0xFFFFFFFF : 0xFFA0A0A0);
		}

		@Override
		protected MutableText getNarrationMessage() {
			return super.getNarrationMessage();
		}

		@Override
		public List<? extends Element> children() {
			return this.widgets;
		}
	}
}
