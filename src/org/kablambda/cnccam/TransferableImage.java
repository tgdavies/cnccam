package org.kablambda.cnccam;

import javax.imageio.ImageIO;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class TransferableImage implements Transferable {

    private File imageFile;

    public TransferableImage(BufferedImage image) {
        try {
            imageFile = File.createTempFile("copy", ".png");

            imageFile.deleteOnExit();
            ImageIO.write(image, "png", imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{
                DataFlavor.javaFileListFlavor
        };
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.match(DataFlavor.javaFileListFlavor);
    }

    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        return Arrays.asList(imageFile);
    }
}