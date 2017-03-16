package com.exalttech.trex.ui.controllers.dashboard.filters;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.Set;

import com.exalttech.trex.ui.PortsManager;
import com.exalttech.trex.util.Initialization;


public class DashboardFilters extends VBox {
    @FXML
    private ComboBox<String> portFilterSelector;
    @FXML
    private VBox portCheckBoxListContainer;
    @FXML
    private ComboBox<Integer> streamsCountSelector;

    private EventHandler<Event> onFiltersChanged;
    private Set<Integer> selectedPortIndexes = new HashSet<>();

    public DashboardFilters() {
        Initialization.initializeFXML(this, "/fxml/Dashboard/filters/DashboardFilters.fxml");

        buildPortsList();
    }

    public EventHandler<Event> getOnFiltersChanged() {
        return onFiltersChanged;
    }

    public void setOnFiltersChanged(EventHandler<Event> onFiltersChanged) {
        this.onFiltersChanged = onFiltersChanged;
    }

    public Set<Integer> getSelectedPortIndexes() {
        return selectedPortIndexes;
    }

    public int getStreamsCount() {
        return streamsCountSelector.getValue();
    }

    @FXML
    public void handlePortFilterSelectorUpdated(Event event) {
        buildPortsList();
    }

    @FXML
    public void handleStreamsCountSelectorUpdated(Event event) {
        handleFiltersUpdated();
    }

    private void handleFiltersUpdated() {
        if (onFiltersChanged != null) {
            onFiltersChanged.handle(new Event(this, null, null));
        }
    }

    private void buildPortsList() {
        selectedPortIndexes.clear();
        portCheckBoxListContainer.getChildren().clear();

        final Set<Integer> portIndexes = getPortIndexes();
        portIndexes.forEach((Integer portIndex) -> {
            selectedPortIndexes.add(portIndex);
            final DashboardPortCheckbox portCheckbox = new DashboardPortCheckbox(
                    portIndex,
                    this::handlePortSelectionChanged
            );
            portCheckBoxListContainer.getChildren().add(portCheckbox);
        });

        handleFiltersUpdated();
    }

    private void handlePortSelectionChanged(Event event) {
        final DashboardPortCheckbox checkBox = (DashboardPortCheckbox) event.getSource();
        if (checkBox.isSelected()) {
            selectedPortIndexes.add(checkBox.getPortNumber());
        } else {
            selectedPortIndexes.remove(checkBox.getPortNumber());
        }

        handleFiltersUpdated();
    }

    private Set<Integer> getPortIndexes() {
        final String val = portFilterSelector.getSelectionModel().getSelectedItem();
        final PortsManager portsManager = PortsManager.getInstance();
        if (val.equals("All")) {
            return new HashSet<>(portsManager.getPortIndexes());
        }
        return new HashSet<>(PortsManager.getInstance().getOwnedPortIndexes());
    }
}