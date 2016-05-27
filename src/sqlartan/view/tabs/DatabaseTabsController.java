package sqlartan.view.tabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import sqlartan.Sqlartan;
import sqlartan.core.Database;
import sqlartan.view.tabs.model.DatabaseStructureModel;
import sqlartan.view.util.Popup;
import java.io.IOException;
import static sqlartan.util.Matching.match;
import static sqlartan.view.util.ActionButtons.actionButton;

/**
 * Created by julien on 30.04.16.
 */
public class DatabaseTabsController extends TabsController {

	@FXML
	private TableColumn<DatabaseStructureModel, Number> colLignes;
	@FXML
	private TableColumn<DatabaseStructureModel, String> colRename;
	@FXML
	private TableColumn<DatabaseStructureModel, String> colDelete;
	@FXML
	private TableView<DatabaseStructureModel> structureTable;
	private Database database;
	private ObservableList<DatabaseStructureModel> dbStructs = FXCollections.observableArrayList();


	/**
	 * Add button rename and drop to structure tab
	 *
	 * @throws IOException
	 */
	@FXML
	protected void initialize() throws IOException {
		super.initialize();

		colLignes.setCellValueFactory(param -> param.getValue().lignes);

		colRename.setCellFactory(actionButton("Rename", (self, event) -> {
			DatabaseStructureModel dbStruct = self.getTableView().getItems().get(self.getIndex());
			String structName = dbStruct.name.get();
			Popup.input("Rename", "Rename " + structName + " into : ", structName).ifPresent(name -> {
				if (name.length() > 0 && !structName.equals(name)) {
					database.structure(structName).ifPresent(s -> Sqlartan.getInstance().getController().renameStructure(s, name));
				}
			});
		}));

		colDelete.setCellFactory(actionButton("Drop", (self, event) -> {
			DatabaseStructureModel dbStruct = self.getTableView().getItems().get(self.getIndex());
			database.structure(dbStruct.name.get()).ifPresent(s -> Sqlartan.getInstance().getController().dropStructure(s));
		}));

		tabPane.getSelectionModel().clearSelection();
	}
	/**
	 * Display the structure of the database
	 */
	protected void displayStructure() {
		dbStructs.clear();
		dbStructs.addAll(database.structures()
		                         .sorted((a, b) -> a.name().compareTo(b.name()))
		                         .map(DatabaseStructureModel::new)
		                         .toList());
		structureTable.setItems(dbStructs);
	}

	/**
	 * {@inheritDoc}
	 * @param newTab
	 */
	@Override
	protected void refresh(Tab newTab) {
		Tab selected = tabPane.getSelectionModel().getSelectedItem();
		if (selected == structureTab) {
			displayStructure();
		}
	}


	/**
	 * Set the database wich will be used in this tab
	 *
	 * @param database the database to work on
	 */
	public void setDatabase(Database database) {
		this.database = database;
	}
}
