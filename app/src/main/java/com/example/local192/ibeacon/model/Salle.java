package com.example.local192.ibeacon.model;

/**
 * Created by local192 on 12/12/2017.
 */

public class Salle {

    private int _major;
    private int _minor;
    private String _name;
    private int _drawable;
    private String _text;
    private boolean _visited;

    public Salle(int major, int minor, String name, int drawable, String text) {
        this._major = major;
        this._minor = minor;
        this._name = name;
        this._drawable = drawable;
        this._text = text;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        this._name = name;
    }

    public int getMajor() {
        return _major;
    }

    public void setMajor(int major) {
        this._major = major;
    }

    public int getMinor() {
        return _minor;
    }

    public void setMinor(int minor) {
        this._minor = minor;
    }

    public int getDrawable() {
        return _drawable;
    }

    public void setDrawable(int drawable) {
        this._drawable = drawable;
    }

    public String getText() {
        return _text;
    }

    public void setText(String text) {
        this._text = text;
    }

    public boolean isVisited() {
        return _visited;
    }

    public void setVisited(boolean _visited) {
        this._visited = _visited;
    }
}
