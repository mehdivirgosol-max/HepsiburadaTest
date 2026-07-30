package com.virgosol.hepsiburada.support;

import com.thoughtworks.gauge.datastore.ScenarioDataStore;
import com.virgosol.hepsiburada.model.SelectedProduct;

public final class ScenarioState {
    private static final String PRODUCT = "selected-product";
    private static final String ADD_ATTEMPTED = "add-attempted";
    private static final String CART_CLEANED = "cart-cleaned";

    private ScenarioState() {
    }

    public static void saveProduct(SelectedProduct product) {
        ScenarioDataStore.put(PRODUCT, product);
    }

    public static SelectedProduct product() {
        Object product = ScenarioDataStore.get(PRODUCT);
        if (!(product instanceof SelectedProduct selectedProduct)) {
            throw new IllegalStateException("Senaryo için henüz bir ürün kaydedilmedi.");
        }
        return selectedProduct;
    }

    public static void markAddAttempted() {
        ScenarioDataStore.put(ADD_ATTEMPTED, true);
    }

    public static boolean wasAddAttempted() {
        return Boolean.TRUE.equals(ScenarioDataStore.get(ADD_ATTEMPTED));
    }

    public static void markCartCleaned() {
        ScenarioDataStore.put(CART_CLEANED, true);
    }

    public static boolean wasCartCleaned() {
        return Boolean.TRUE.equals(ScenarioDataStore.get(CART_CLEANED));
    }
}
