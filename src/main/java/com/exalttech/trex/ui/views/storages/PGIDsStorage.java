package com.exalttech.trex.ui.views.storages;

import javafx.concurrent.WorkerStateEvent;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cisco.trex.stateless.gui.services.ActivePGIDsService;


public class PGIDsStorage {
    public interface PGIDsChangedListener {
        void pgIDsChanged();
    }

    public interface SelectedPGIDsChangedListener {
        void selectedPGIDsChanged();
    }

    public interface SelectedPGIDsInitializedListener {
        void selectedPGIDsInitialized();
    }

    private static final Duration POLLING_INTERVAL = Duration.seconds(1);

    private final Map<String, Boolean> legendColorMap = new LinkedHashMap<String, Boolean>(){{
        put("#f3622d", false);
        put("#fba71b", false);
        put("#57b757", false);
        put("#41a9c9", false);
        put("#4258c9", false);
        put("#9a42c8", false);
        put("#c84164", false);
        put("#888888", false);
    }};

    private final ActivePGIDsService activePGIDsService = new ActivePGIDsService();
    private Set<Integer> pgIDs = new HashSet<>();
    private Map<Integer, String> selectedPGIds = null;
    private final Object dataLock = new Object();
    private final List<PGIDsChangedListener> pgIDsChangedListeners = new ArrayList<>();
    private final List<SelectedPGIDsChangedListener> selectedPGIDsChangedListeners = new ArrayList<>();
    private final List<SelectedPGIDsInitializedListener> selectedPGIDsInitializedListeners = new ArrayList<>();

    public Object getDataLock() {
        return dataLock;
    }

    public Set<Integer> getPgIDs() {
        return pgIDs;
    }

    public Map<Integer, String> getSelectedPGIds() {
        return selectedPGIds;
    }

    public void startPolling() {
        synchronized (activePGIDsService) {
            if (!activePGIDsService.isRunning()) {
                activePGIDsService.setPeriod(POLLING_INTERVAL);
                activePGIDsService.setOnSucceeded(this::handlePGIDsReceived);
                activePGIDsService.start();
            }
        }
    }

    public void stopPolling() {
        synchronized (activePGIDsService) {
            if (activePGIDsService.isRunning()) {
                activePGIDsService.cancel();
                activePGIDsService.reset();
            }
        }

        synchronized (dataLock) {
            pgIDs.clear();
            selectedPGIds = null;
        }
    }

    public void selectPGID(final Integer pgID) {
        synchronized (dataLock) {
            if (selectedPGIds == null) {
                selectedPGIds = new HashMap<>();
            }

            selectedPGIds.put(pgID, holdColor());
        }

        handleSelectedPGIDsChanged();
    }

    public void deselectPGID(final Integer pgID) {
        synchronized (dataLock) {
            if (selectedPGIds == null) {
                return;
            }

            freeColor(selectedPGIds.get(pgID));
            selectedPGIds.remove(pgID);
        }

        handleSelectedPGIDsChanged();
    }

    public void addSelectedPGIDsChangedListener(final SelectedPGIDsChangedListener listener) {
        synchronized (selectedPGIDsChangedListeners) {
            selectedPGIDsChangedListeners.add(listener);
        }
    }

    public void removeSelectedPGIDsChangedListener(final SelectedPGIDsChangedListener listener) {
        synchronized (selectedPGIDsChangedListeners) {
            selectedPGIDsChangedListeners.remove(listener);
        }
    }

    public void addPGIDsChangedListener(final PGIDsChangedListener listener) {
        synchronized (pgIDsChangedListeners) {
            pgIDsChangedListeners.add(listener);
        }
    }

    public void removePGIDsChangedListener(final PGIDsChangedListener listener) {
        synchronized (pgIDsChangedListeners) {
            pgIDsChangedListeners.remove(listener);
        }
    }

    public void addSelectedPGIDsInitializedListener(final SelectedPGIDsInitializedListener listener) {
        synchronized (selectedPGIDsInitializedListeners) {
            selectedPGIDsInitializedListeners.add(listener);
        }
    }

    public void removeSelectedPGIDsInitializedListener(final SelectedPGIDsInitializedListener listener) {
        synchronized (selectedPGIDsInitializedListeners) {
            selectedPGIDsInitializedListeners.remove(listener);
        }
    }

    private void handlePGIDsReceived(final WorkerStateEvent event) {
        final ActivePGIDsService service = (ActivePGIDsService) event.getSource();
        final Set<Integer> receivedPGIds = service.getValue();

        if (receivedPGIds == null) {
            return;
        }

        boolean selectedPGIDsInitialized = false;

        synchronized (dataLock) {
            if (pgIDs.equals(receivedPGIds)) {
                return;
            }

            pgIDs = receivedPGIds;

            if (!pgIDs.isEmpty() && selectedPGIds == null) {
                selectedPGIds = new HashMap<>();
                int count = 4;
                for (final int pgid : pgIDs) {
                    selectedPGIds.put(pgid, holdColor());
                    if (--count == 0) {
                        break;
                    }
                }
                selectedPGIDsInitialized = true;
            }
        }

        if (selectedPGIDsInitialized) {
            handleSelectedPGIDsInitialized();
        } else {
            handlePGIDsChanged();
        }
    }

    private void handleSelectedPGIDsChanged() {
        synchronized (selectedPGIDsChangedListeners) {
            selectedPGIDsChangedListeners.forEach(SelectedPGIDsChangedListener::selectedPGIDsChanged);
        }
    }

    private void handlePGIDsChanged() {
        synchronized (pgIDsChangedListeners) {
            pgIDsChangedListeners.forEach(PGIDsChangedListener::pgIDsChanged);
        }
    }

    private void handleSelectedPGIDsInitialized() {
        synchronized (selectedPGIDsInitializedListeners) {
            selectedPGIDsInitializedListeners.forEach(SelectedPGIDsInitializedListener::selectedPGIDsInitialized);
        }
    }

    private String holdColor() {
        for (final Map.Entry<String, Boolean> entry : legendColorMap.entrySet()) {
            if (!entry.getValue()) {
                entry.setValue(true);
                return entry.getKey();
            }
        }
        return null;
    }

    private void freeColor(final String color) {
        legendColorMap.put(color, false);
    }
}
