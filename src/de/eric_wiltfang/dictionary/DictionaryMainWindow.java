package de.eric_wiltfang.dictionary;
import java.awt.EventQueue;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTable;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory;

import de.eric_wiltfang.dictionary.csv.CSVExporterDialog;
import de.eric_wiltfang.dictionary.csv.CSVImporterDialog;
import de.eric_wiltfang.dictionary.html.HTMLExporter;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import java.awt.Point;

import javax.swing.JProgressBar;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DictionaryMainWindow {

	private JFrame frmDictionaryEditor;
	private JTextField searchField;
	private JTable table;
	private EntryTableModel model;
	private JLabel lblStatuslabel;
	
	private Dictionary dic;
	private File defaultFile;
	private boolean changed;
	
	private Vector<JComponent> needDictionary;
	private JLabel lblEntries;
	private JProgressBar progressBar;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(
						UIManager.getSystemLookAndFeelClassName()
					);
				} catch (Exception ex) {
					// No biggie, default style works too.
				}
				try {
					DictionaryMainWindow window = new DictionaryMainWindow();
					window.frmDictionaryEditor.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public DictionaryMainWindow() {
		needDictionary = new Vector<>();
		initialize();
		
		table.setModel(model);
		
		JPopupMenu popupMenu = new JPopupMenu();
		addPopup(table, popupMenu);
		
		JMenuItem mntmNewEntryRightClick = new JMenuItem("New entry");
		mntmNewEntryRightClick.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				EditEntryWindow window = new EditEntryWindow(dic);
				window.setVisible(true);
			}
		});
		popupMenu.add(mntmNewEntryRightClick);
		needDictionary.add(mntmNewEntryRightClick);
		
		JMenuItem mntmEditSelectedRightClick = new JMenuItem("Edit selected");
		mntmEditSelectedRightClick.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				editSelected();
			}
		});
		popupMenu.add(mntmEditSelectedRightClick);
		needDictionary.add(mntmEditSelectedRightClick);
		
		JMenuItem mntnDeleteRightClick = new JMenuItem("Delete selected");
		mntnDeleteRightClick.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				deleteSelected();
			}
		});
		popupMenu.add(mntnDeleteRightClick);
		needDictionary.add(mntnDeleteRightClick);
		
		setComponentEnabled(false);
		setStatus("Welcome", false);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmDictionaryEditor = new JFrame();
		frmDictionaryEditor.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quit();
			}
		});
		frmDictionaryEditor.setTitle("Dictionary Editor");
		frmDictionaryEditor.setBounds(100, 100, 535, 468);
		frmDictionaryEditor.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		frmDictionaryEditor.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!askForSave()) {
					return;
				}
				
				EditDictionarySettingsDialog dialog = new EditDictionarySettingsDialog();
				dialog.setVisible(true);
				if (dialog.getConfirmed()) {
					try {
						Dictionary dic = Dictionary.createNew(dialog.getSettings());
						setDictionary(dic);
						setStatus("Created new dictionary.", false);
						
						changed = true;
					} catch (Exception ex) {
						showError("Error while creating new dictionary", ex);
					}
					frmDictionaryEditor.setTitle("Dictionary Editor: " + dic.getName());
					defaultFile = null;
				}
			}
		});
		mnFile.add(mntmNew);
		
		JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!askForSave()) {
					return;
				}
				
				CostumFileChooser chooser = new CostumFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Dictionary file", "dict");
				chooser.setFileFilter(filter);
				chooser.setDialogType(JFileChooser.OPEN_DIALOG);
				
				if (chooser.showOpenDialog(frmDictionaryEditor) == CostumFileChooser.APPROVE_OPTION) {
					try {
						setStatus("Loading", true);
						Dictionary dic = Dictionary.createFromFile(chooser.getSelectedFile());
						frmDictionaryEditor.setTitle("Dictionary Editor: " + dic.getName());
						defaultFile = chooser.getSelectedFile();
						setDictionary(dic);
						changed = false;
						setStatus("Loaded successfully.", false);
					} catch (Exception ex) {
						showError("Error while loading dictionary", ex);
					}
				}
			}
		});
		mntmOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mnFile.add(mntmOpen);
		
		JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});
		mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mnFile.add(mntmSave);
		needDictionary.add(mntmSave);
		
		JMenuItem mntmSaveAs = new JMenuItem("Save as");
		mntmSaveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File temp = defaultFile;
				defaultFile = null;
				save();
				if (defaultFile == null) {
					defaultFile = temp;
				}
			}
		});
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mnFile.add(mntmSaveAs);
		needDictionary.add(mntmSaveAs);
		
		JSeparator separator_1 = new JSeparator();
		mnFile.add(separator_1);
		
		JMenu mnImport = new JMenu("Import...");
		mnFile.add(mnImport);
		
		JMenuItem mntmCSVImport = new JMenuItem("Table (.csv)");
		mntmCSVImport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				CSVImporterDialog importer = new CSVImporterDialog(dic);
				importer.setVisible(true);
			}
		});
		mnImport.add(mntmCSVImport);
		needDictionary.add(mntmCSVImport);
		
		JMenu mnExportAs = new JMenu("Export as...");
		mnFile.add(mnExportAs);
		needDictionary.add(mnExportAs);
		
		JMenuItem mntmCSVExport = new JMenuItem("Table (.csv)");
		mntmCSVExport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CSVExporterDialog dialog = new CSVExporterDialog(dic);
				dialog.setVisible(true);
			}
		});
		mnExportAs.add(mntmCSVExport);
		needDictionary.add(mntmCSVExport);
		
		JMenuItem mntmWebDictionaryhtml = new JMenuItem("Web dictionary (.html)");
		mntmWebDictionaryhtml.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CostumFileChooser chooser = new CostumFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Webpage", "html", "htm");
				chooser.setFileFilter(filter);
				if (!dic.getName().isEmpty()) {
					chooser.setSelectedFile(new File(dic.getName() + ".html"));
				}
				
				if (chooser.showSaveDialog(frmDictionaryEditor) == CostumFileChooser.APPROVE_OPTION) {
					File f = chooser.getSelectedFile();
					try {
						HTMLExporter exp = new HTMLExporter(f);
						dic.export(exp);
					} catch (Exception ex) {
						ErrorDialog.showError("Error while exporting", ex);
					}
				} else {
					return;
				}
			}
		});
		mnExportAs.add(mntmWebDictionaryhtml);
		needDictionary.add(mntmWebDictionaryhtml);
		
		JMenuItem mntmQuit = new JMenuItem("Quit");
		mntmQuit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				quit();
			}
		});
		
		JSeparator separator = new JSeparator();
		mnFile.add(separator);
		mntmQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
		mnFile.add(mntmQuit);
		
		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);
		
		JMenuItem mntmDeleteSelected = new JMenuItem("Delete selected");
		mntmDeleteSelected.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				deleteSelected();
			}
		});
		
		JMenuItem mntmNewEntry = new JMenuItem("New entry");
		mntmNewEntry.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				EditEntryWindow window = new EditEntryWindow(dic);
				window.setVisible(true);
			}
		});
		mntmNewEntry.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
		mnEdit.add(mntmNewEntry);
		needDictionary.add(mntmNewEntry);
		
		JMenuItem mntmMultipleNewEntries = new JMenuItem("Multiple new entries");
		mntmMultipleNewEntries.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				EditEntryWindow window = new EditEntryWindow(dic);
				window.setNewEntryOnSave(true);
				window.setVisible(true);
			}
		});
		mntmMultipleNewEntries.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mnEdit.add(mntmMultipleNewEntries);
		needDictionary.add(mntmMultipleNewEntries);
		
		JSeparator separator_3 = new JSeparator();
		mnEdit.add(separator_3);
		
		JMenuItem mntmEditSelected = new JMenuItem("Edit selected");
		mntmEditSelected.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editSelected();
			}
		});
		mntmEditSelected.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
		mnEdit.add(mntmEditSelected);
		needDictionary.add(mntmEditSelected);
		mntmDeleteSelected.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		mnEdit.add(mntmDeleteSelected);
		needDictionary.add(mntmDeleteSelected);
		
		JSeparator separator_2 = new JSeparator();
		mnEdit.add(separator_2);
		
		JMenuItem mntmEditDictionarySettings = new JMenuItem("Edit dictionary settings");
		mntmEditDictionarySettings.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				EditDictionarySettingsDialog edsd = new EditDictionarySettingsDialog(dic.getSettings());
				edsd.setVisible(true);
				if (edsd.getConfirmed()) {
					dic.setSettings(edsd.getSettings());
				}
				
				frmDictionaryEditor.setTitle("Dictionary: " + dic.getName());
				changed = true;
			}
		});
		mnEdit.add(mntmEditDictionarySettings);
		needDictionary.add(mntmEditDictionarySettings);
		
//		JMenu mnDebug = new JMenu("Debug");
//		menuBar.add(mnDebug);
//		
//		JMenuItem mntmInsertgarbage = new JMenuItem("InsertGarbage");
//		mntmInsertgarbage.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				try {
//					int num = Integer.parseInt(JOptionPane.showInputDialog("How much garbage are we talking about here...?"));
//					dic.insertJunk(num);
//				} catch (Exception ex) {
//					showError("Garbage error", ex);
//				}
//			}
//		});
//		mnDebug.add(mntmInsertgarbage);
		
		frmDictionaryEditor.getContentPane().setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("center:default:grow"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("fill:default:grow"),
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		
		JLabel lblSearch = new JLabel("Search");
		frmDictionaryEditor.getContentPane().add(lblSearch, "2, 2, right, default");
		
		searchField = new JTextField();
		searchField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String key = searchField.getText();
				if (dic != null) {
					try {
						model.searchFor(key);
					} catch (SQLException ex) {
						showError("Internal error", ex);
					}
				}
			}
		});
		frmDictionaryEditor.getContentPane().add(searchField, "4, 2, fill, default");
		searchField.setColumns(10);
		needDictionary.add(searchField);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		frmDictionaryEditor.getContentPane().add(scrollPane, "2, 4, 3, 1, fill, fill");

		model = new EntryTableModel();
		table = new JTable();
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent ev) {
				if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
					editSelected();
				}
			}
		});
		table.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
					Point p = e.getPoint();
					if (table.isRowSelected(table.rowAtPoint(p))) {
						editSelected();
					}
				}
			}
		});
		table.setFillsViewportHeight(true);
		table.setEnabled(false);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setModel(model);
		table.getColumnModel().getColumn(0).setPreferredWidth(100);
		table.getColumnModel().getColumn(1).setPreferredWidth(200);
		table.getColumnModel().getColumn(3).setPreferredWidth(100);
		scrollPane.setViewportView(table);
		needDictionary.add(table);
		
		JPanel panel = new JPanel();
		frmDictionaryEditor.getContentPane().add(panel, "1, 6, 4, 1, default, fill");
		panel.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("43px:grow"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("right:97px"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("right:min"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,},
			new RowSpec[] {
				RowSpec.decode("14px"),}));
		
		lblStatuslabel = new JLabel("Statuslabel");
		panel.add(lblStatuslabel, "2, 1, left, top");
		
		progressBar = new JProgressBar();
		panel.add(progressBar, "4, 1, left, center");
		progressBar.setVisible(false);
		
		lblEntries = new JLabel("0 entries");
		panel.add(lblEntries, "6, 1, right, center");
		model.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				lblEntries.setText(((EntryTableModel)e.getSource()).getRowCount() + " entries");
			}
		});
	}
	
	/**
	 * Enables/disables all components that need a loaded dictionary to work.
	 */
	private void setComponentEnabled(boolean b) {
		for (JComponent c : needDictionary) {
			c.setEnabled(b);
		}
	}
	/**
	 * Sets the dictionary the program edits.
	 * @param dic The dictionary to edit.
	 */
	private void setDictionary(Dictionary dic) {
		this.dic = dic;
		
		dic.addDictionaryListener(new DictionaryListener() {
			public void recieveEvent(DictionaryEvent event) {
				changed = true;
			}
		});
		dic.addDictionaryListener(new DictionaryListener() {
			public void recieveEvent(DictionaryEvent event) {
				switch (event.getType()) {
				case OTHER:
				case UPDATE:
					setStatus("Edited dictionary", false);
					break;
				case DELETE:
					setStatus("Deleted entry", false);
					break;
				case NEW:
					setStatus("Added new entry", false);
					break;
				}
			}
		});
		
		setComponentEnabled(true);
		
		connectTableModel();
	}
	/**
	 * Sets up the table model with the current dictionary.
	 */
	private void connectTableModel() {
		try {
			model.setDictionary(dic);
		} catch (Exception ex) {
			showError("Internal error", ex);
		}
		try {
			model.searchFor(searchField.getText());
		} catch (Exception ex) {
			showError("Internal error", ex);
		}
	}
	
	private void showError(String message, Exception ex) {
		ErrorDialog.showError(message, ex);
	}
	private Vector<Long> getSelectedIDs() {
		int[] rows = table.getSelectedRows();
		Vector<Long> ids = new Vector<>(rows.length);
		for (int row : rows) {
			ids.add(model.getID(row));
		}
		return ids;
	}
	private void deleteSelected() {
		if (JOptionPane.showConfirmDialog(frmDictionaryEditor, "Are you sure you want to delete the selected entries?") != JOptionPane.OK_OPTION) {
			return;
		}
		try {
			for (Long id : getSelectedIDs()) {
				dic.deleteEntry(id);
			}
		} catch (Exception ex) {
			showError("Error while deleting", ex);
		}
	}
	private void editSelected() {
		if (table.getSelectedRowCount() > 10) {
			if (JOptionPane.showConfirmDialog(frmDictionaryEditor, "Are you sure you want to edit " + table.getSelectedRowCount() +
					" entries? This will open a lot of windows.", "Mass edit?", 
					JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
				return;
			}
		}
		try {
			for (Long id : getSelectedIDs()) {
				EditEntryWindow editWindow = new EditEntryWindow(dic, dic.getEntry(id));
				editWindow.setVisible(true);
			}
		} catch (Exception ex) {
			showError("Internal error", ex);
		}
	}
	private void quit() {
		if (!askForSave()) {
			return;
		}
		
		if (dic != null) {
			try {
				dic.cleanup();
			} catch (IOException ex) {
				showError("Error while cleaning up temporary files, there might be temporary files left", ex);
			}
		}
		System.exit(0);
	}
	/**
	 * Asks the user if they want to save if there was a change.
	 * @return false if the action was cancelled, true if the file was saved or the user declined saving (or there was nothing to save).
	 */
	private boolean askForSave() {
		if (changed) {
			int answer = JOptionPane.showConfirmDialog(frmDictionaryEditor, "Do you want to save before closing this dictionary?", "Save?", JOptionPane.YES_NO_CANCEL_OPTION);
			
			switch (answer) {
			case JOptionPane.YES_OPTION:
				boolean saved = save();
				return saved;
			case JOptionPane.NO_OPTION:
				return true;
			case JOptionPane.CANCEL_OPTION:
				return false;
			}
		}
		return true;
	}
	/**
	 * Shows a dialog to save the dictionary.
	 * @return true if the dictionary was saved, false otherwise.
	 */
	private boolean save() {
		if (dic == null) {
			return false;
		}
		File f;
		if (defaultFile == null) {
			CostumFileChooser chooser = new CostumFileChooser();
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Dictionary file", "dict");
			chooser.setFileFilter(filter);
			chooser.setSelectedFile(new File(dic.getName() + ".dict"));
			chooser.setDialogType(JFileChooser.SAVE_DIALOG);
			
			if (chooser.showSaveDialog(frmDictionaryEditor) == CostumFileChooser.APPROVE_OPTION) {
				f = chooser.getSelectedFile();
				System.out.println(f);
			} else {
				return false;
			}
		} else {
			f = defaultFile;
		}
		try {
			setStatus("Saving", true);
			dic.save(f);
			defaultFile = f;
			setStatus("Saved successfully.", false);
			
			changed = false;
			return true;
		} catch (Exception ex) {
			showError("Error while saving dictionary", ex);
			return false;
		}
	}
	
	private void setStatus(String message, boolean busy) {
		lblStatuslabel.setText(message + (busy?"...":""));
		//progressBar.setIndeterminate(busy);
	}
	
	private static void addPopup(Component component, final JPopupMenu popup) {
		component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			private void showMenu(MouseEvent e) {
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}
}