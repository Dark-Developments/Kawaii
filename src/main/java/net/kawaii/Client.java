package net.kawaii;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.ethanol.ethanolapi.server.listener.EthanolServerListener;

import java.io.File;

public class Client implements ClientModInitializer {
    public static final Client INSTANCE = new Client();

    public static MinecraftClient mc = MinecraftClient.getInstance();

    public final String NAME = "Kawaii";
    public final String MODID = "kawaii";
    public static final File BASEFILE = new File(MinecraftClient.getInstance().runDirectory, INSTANCE.MODID);
    public static final Logger LOGGER = LoggerFactory.getLogger(INSTANCE.NAME);


    public static EthanolServerListener EthanolListener;


    @Override
    public void onInitializeClient() {
     System.out.println("""
               
               _
              {_}
              |(|
              |=|
             /   \\
             |.--|  
             ||  | 
             ||  | 
             |'--|
             '-=-'
             
             """);

    }
}
