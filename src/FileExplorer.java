import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class FileExplorer {

    public static final String APP_TITLE = "File Explorer";
    private FileSystemView fileSystemView;

    private JPanel gui;
    private JTree tree;
    private JTable table;
    private FileTable fileTable;
    private ListSelectionListener listSelectionListener;
    private boolean cellSizesSet = false;
    private final int rowIconPadding = 10;
    private JTextField path;
    private File file;
    private File dirTo;
    private File back;
    private File previousPath;
    private String nameFile;

    public Container getGui() {
        if (gui == null) {
            gui = new JPanel();
            gui.setMinimumSize(new Dimension(0, 0));
            gui.setBorder(new EmptyBorder(5, 5, 5, 5));

            fileSystemView = FileSystemView.getFileSystemView();
            Desktop.getDesktop();

            JPanel detailView = new JPanel();
            detailView.setRequestFocusEnabled(false);
            detailView.setOpaque(false);
            detailView.setMinimumSize(new Dimension(600, 220));
            detailView.setIgnoreRepaint(true);
            detailView.setAutoscrolls(true);

            table = new JTable();
            table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.setShowVerticalLines(false);

            listSelectionListener = lse -> {
                int row = table.getSelectionModel().getLeadSelectionIndex();
                setFileDetails(((FileTable) table.getModel()).getFile(row));
            };
            table.getSelectionModel().addListSelectionListener(listSelectionListener);
            JScrollPane tableScroll = new JScrollPane(table);
            tableScroll.setBounds(0, 42, 746, 273);
            Dimension d = tableScroll.getPreferredSize();
            detailView.setLayout(null);

            JPanel fileMainDetails = new JPanel();
            fileMainDetails.setBounds(0, 0, 746, 37);
            detailView.add(fileMainDetails);
            fileMainDetails.setBorder(new EmptyBorder(0, 6, 0, 6));
            fileMainDetails.setLayout(null);

            JPanel fileDetailsLabels = new JPanel();
            fileDetailsLabels.setBounds(6, 0, 0, 0);
            fileMainDetails.add(fileDetailsLabels);
            fileDetailsLabels.setLayout(null);

            JPanel fileDetailsValues = new JPanel();
            fileDetailsValues.setBounds(0, 2, 742, 35);
            fileMainDetails.add(fileDetailsValues);
            fileDetailsValues.setLayout(null);
            path = new JTextField(29);
            path.setBounds(28, 6, 424, 22);
            fileDetailsValues.add(path);
            path.setHorizontalAlignment(SwingConstants.LEFT);

            int count = fileDetailsLabels.getComponentCount();
            tableScroll.setPreferredSize(new Dimension((int) d.getWidth(), (int) d.getHeight() / 2));
            detailView.add(tableScroll);

            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            DefaultTreeModel treeModel = new DefaultTreeModel(root);

            TreeSelectionListener treeSelectionListener = event -> {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                showChildren(node);
                setFileDetails((File) node.getUserObject());
            };

            File[] roots = fileSystemView.getRoots();

            for (File fileSystemRoot : roots) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
                root.add(node);  // Thêm node con vào root node
                File[] files = fileSystemView.getFiles(fileSystemRoot, true);
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Thêm node con vào root node
                        node.add(new DefaultMutableTreeNode(file));
                    }
                }
            }

            // Tạo cây
            createTree(treeModel, treeSelectionListener);

            // Tree Scroll
            JScrollPane treeScroll = new JScrollPane(tree);
            treeScroll.setMinimumSize(new Dimension(100, 27));
            Dimension preferredSize = treeScroll.getPreferredSize();
            Dimension widePreferred = new Dimension(200, (int) preferredSize.getHeight());
            treeScroll.setPreferredSize(widePreferred);
            for (int i = 0; i < count; i++) {
                fileDetailsLabels.getComponent(i).setEnabled(false);
            }

            /**
             * Tạo panel chứa các button ở phía dưới
             */
            JPanel fileView = new JPanel();
            fileView.setBounds(0, 315, 746, 50);

            detailView.add(fileView);

            createToolBar(fileView);
            gui.repaint();
            gui.setLayout(null);

            JPanel panel = new JPanel();
            treeScroll.setColumnHeaderView(panel);
            panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

            // Tạo nút back
            panel.add(createBackButton());


            // Chia panel làm 2: Trái - cây folder, Phải - chi tiết Foler
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailView);
            splitPane.setBounds(5, 0, 965, 373);
            gui.add(splitPane);
        }
        return gui;
    }

    // Tạo tree thư mục
    private void createTree(DefaultTreeModel treeModel, TreeSelectionListener treeSelectionListener) {
        tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setCellRenderer(new FileTreeCellRenderer());
        tree.expandRow(0);
        tree.addTreeSelectionListener(treeSelectionListener);
    }

    private JButton createBackButton() {
        JButton btnBack = new JButton("Back");
        btnBack.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    back = new File(path.getText());
                    if (back.getParent() == null) {
                        back = new File(path.getText());
                    } else {
                        previousPath = new File(back.getParent());
                        DefaultMutableTreeNode node = new DefaultMutableTreeNode(previousPath);
                        showChildren(node);
                        path.setText(back.getParent());
                    }
                }
            }
        });

        return btnBack;
    }

    // Tạo các button copy, paste, open
    private void createToolBar(JPanel fileView) {
        JToolBar toolBar = new JToolBar();
        toolBar.setBounds(0, 0, 746, 48);
        fileView.add(toolBar);
        toolBar.setFloatable(false);

        // Button open
        JButton btnOpen = Button.createSimpleButton("Open Folder/File");
        btnOpen.addActionListener(action -> {
            File open = new File(path.getText());
            try {
                if (open.isFile()) {
                    openFile(open);
                } else if (open.isDirectory()) {
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(open);
                    showChildren(node);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        toolBar.add(btnOpen);

        // Button copy
        JButton btnCopy = Button.createSimpleButton("Copy");
        btnCopy.addActionListener(action -> {
            file = new File(path.getText());
            String name = path.getText();
            String[] words = name.split("\\\\");
            for (String w : words) {
                nameFile = w;
            }
        });
        toolBar.add(btnCopy);

        // Button paste
        JButton btnPaste = Button.createSimpleButton("Paste");
        btnPaste.addActionListener(action -> {
            if (file.isDirectory()) {
                dirTo = new File(path.getText() + "\\" + nameFile);
                FileExplorer.copyFolder(file, dirTo);
            } else if (file.isFile()) {
                dirTo = new File(path.getText() + "\\" + nameFile);
                try {
                    copyFile(file, dirTo);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        toolBar.add(btnPaste);

        // Button delete
        JButton btnDelete = Button.createSimpleButton("Delete");
        btnDelete.addActionListener(action -> {
            file = new File(path.getText());
            deleteDir(file);
        });
        toolBar.add(btnDelete);


        // Button rename
        JButton btnRename = Button.createSimpleButton("Rename");
        btnRename.addActionListener(action -> {
            String newName = JOptionPane.showInputDialog("Please input your name");
            if ("".equals(newName)) {
                JOptionPane.showConfirmDialog(null, "Please input something...");
            } else {
                file = new File(path.getText());
                String oldPath = path.getText();
                // get extension
                String extension = "";
                int i = oldPath.lastIndexOf('.');
                if (i > 0) {
                    extension = oldPath.substring(i + 1);
                }

                String newPath = file.getAbsolutePath().replace(file.getName(), "") + newName;

                if (!extension.equals("")) {
                    newPath += "." + extension;
                }
                boolean renameSuccess = file.renameTo(new File(newPath));

                if (renameSuccess) {
                    JOptionPane.showMessageDialog(null, "Rename successfully!");
                } else
                    JOptionPane.showMessageDialog(null, "Rename failed!");
            }
        });
        toolBar.add(btnRename);
    }

    public static void copyFile(File oldLocation, File newLocation) throws IOException {
        if (oldLocation.exists()) {
            BufferedInputStream reader = new BufferedInputStream(new FileInputStream(oldLocation));
            BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(newLocation, false));
            try {
                byte[] buff = new byte[8192];
                int numChars;
                while ((numChars = reader.read(buff, 0, buff.length)) != -1) {
                    writer.write(buff, 0, numChars);
                }
            } catch (IOException ex) {
                throw new IOException(ex.getMessage());
            } finally {
                try {
                    writer.close();
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            throw new IOException("Something error");
        }
    }

    public static void copyFolder(File source, File destination) {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }

            String[] files = source.list();

            for (String file : files) {
                File srcFile = new File(source, file);
                File destFile = new File(destination, file);

                copyFolder(srcFile, destFile);
            }
        } else {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new FileInputStream(source);
                out = new FileOutputStream(destination);

                byte[] buffer = new byte[1024];

                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            } catch (Exception e) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public static void openFile(File path) throws IOException {
        if (!Desktop.isDesktopSupported()) {
            System.out.println("Desktop is not supported");
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (path.exists())
            desktop.open(path);
    }

    public void showRootFile() {
        tree.setSelectionInterval(0, 0);
    }

    private void setTableData(final File[] files) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (fileTable == null) {
                    fileTable = new FileTable();
                    table.setModel(fileTable);
                }
                table.getSelectionModel().removeListSelectionListener(listSelectionListener);
                fileTable.setFiles(files);
                table.getSelectionModel().addListSelectionListener(listSelectionListener);
                if (!cellSizesSet) {
                    Icon icon = fileSystemView.getSystemIcon(files[0]);

                    table.setRowHeight(icon.getIconHeight() + rowIconPadding);

                    setColumnWidth(0, -1);
                    setColumnWidth(3, 60);
                    table.getColumnModel().getColumn(3).setMaxWidth(120);

                    cellSizesSet = true;
                }

            }
        });
    }

    private void setColumnWidth(int column, int width) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        if (width < 0) {
            JLabel label = new JLabel((String) tableColumn.getHeaderValue());
            Dimension preferred = label.getPreferredSize();
            width = (int) preferred.getWidth() + 14;
        }
        tableColumn.setPreferredWidth(width);
        tableColumn.setMaxWidth(width);
        tableColumn.setMinWidth(width);
    }

    private void showChildren(final DefaultMutableTreeNode node) {
        tree.setEnabled(false);

        SwingWorker<Void, File> worker = new SwingWorker<Void, File>() {
            @Override
            public Void doInBackground() {
                File file = (File) node.getUserObject();
                if (file.isDirectory()) {
                    File[] files = fileSystemView.getFiles(file, true);
                    if (node.isLeaf()) {
                        for (File child : files) {
                            if (child.isDirectory()) {
                                publish(child);
                            }
                        }
                    }
                    setTableData(files);
                }
                return null;
            }

            @Override
            protected void process(List<File> chunks) {
                for (File child : chunks) {
                    node.add(new DefaultMutableTreeNode(child));
                }
            }

            @Override
            protected void done() {
                tree.setEnabled(true);
            }
        };

        worker.execute();
    }

    private void setFileDetails(File file) {
        path.setText(file.getPath());

        JFrame f = (JFrame) gui.getTopLevelAncestor();
        if (f != null) {
            f.setTitle(APP_TITLE + " - " + fileSystemView.getSystemDisplayName(file));
        }

        gui.repaint();
    }

    void deleteDir(File file) {
        File[] listFiles = file.listFiles();
        if (listFiles != null) {
            for (File f : listFiles) {
                if (!Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }

}
