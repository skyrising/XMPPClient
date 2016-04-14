package de.skyrising.xmpp.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class MainWindow extends JFrame {
    public MainWindow() throws IOException {
        this.setIconImage(ImageIO.read(MainWindow.class.getResourceAsStream("/xmpp2.png")));
        this.setTitle("XMPP Client");
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setVisible(true);
    }
}
