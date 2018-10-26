import lombok.ToString;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ToString
public class JsonObject {
    private String project;
    private String text;
    private String model;
    private Intent intent;
    private Intent[] intent_ranking;
    private Entity[] entities;
}
