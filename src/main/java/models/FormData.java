package models;

/**
 *
 * @author Eduard Tomàs
 */

public class FormData {
    private String name;
    private String email;
    private String notes;

    public FormData() {

    }

    public FormData(String name, String email, String notes)
    {
        this.name = name;
        this.email = email;
        this.notes = notes;
    }

    public String getName()
    {
        return name;
    }

    public String getEmail()
    {
        return email;
    }

    public String getNotes()
    {
        return notes;
    }
}
