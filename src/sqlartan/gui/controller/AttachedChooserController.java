package sqlartan.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import sqlartan.gui.util.Popup;
import java.io.File;

/**
 * Controller for the AttachedChooser fxml file.
 * Used to give the user an inferface to attache a database
 * to the main one.
 */
public class AttachedChooserController {

	File file = null;
	private SqlartanController controller;

	@FXML
	private Button ok;
	@FXML
	private Button cancel;
	@FXML
	private Button browse;
	@FXML
	private TextField path;
	@FXML
	private Pane attachedPane;
	@FXML
	private TextField dbName;


	/**
	 * Close the window
	 */
	@FXML
	private void close() {
		((Stage) attachedPane.getScene().getWindow()).close();
	}

	public void setController(SqlartanController controller) {
		this.controller = controller;
	}


	/**
	 * Method called by the validate button
	 */
	@FXML
	protected void validate() {
		file = new File(path.getText());

		if (!file.getPath().isEmpty() && !dbName.getText().isEmpty()) {
			controller.attachDatabase(file, dbName.getText());
			close();
		} else {
			Popup.error("Invalid Entry", "Informations for the path and/or the database name are empty");
		}
	}

	/**
	 * Method called by the browse button
	 * To browse and check the path the user has set
	 */
	@FXML
	protected void browse() {
		String oldPath = path.getText();

		Popup.browse("Select database to attach", attachedPane.getScene().getWindow(), null)
		     .ifPresent(file -> {
			     if (dbName.getText().isEmpty() || (!oldPath.isEmpty() && fileName(new File(oldPath)).equals(dbName.getText()))) {
				     dbName.setText(fileName(file));
			     }
			     path.setText(file.getPath());
		     });
	}

	private String fileName(File file) {
		return file.getName().split("\\.")[0];
	}
}
