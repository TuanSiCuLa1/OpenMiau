package myau.config.online;

public class OnlineConfigEntry {
    public String setting_id;
    public String name;
    public String setting_type;
    public String description;
    public String date;
    public String contributors;
    public String status_type;
    public String status_date;
    public String version;

    public String getId() {
        return setting_id == null ? "" : setting_id;
    }

    public String getName() {
        return name == null || name.trim().isEmpty() ? getId() : name;
    }

    public String getAuthor() {
        return contributors == null || contributors.trim().isEmpty() ? "unknown" : contributors;
    }

    public String getVersion() {
        return version == null || version.trim().isEmpty() ? "" : version;
    }
}
