package models;

import i18n.I18n;

public enum LineCategory {
    MATERIAL("line_category.material"),
    SERVEI("line_category.service");

    private final String labelKey;

    LineCategory(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getLabel() {
        return I18n.t(labelKey);
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
