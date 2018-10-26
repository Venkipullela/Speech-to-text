import lombok.ToString;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ToString
public class Entity {
    private String entity;
    private String value;
    private String extractor;
    private int start;
    private int end;
    private float confidence;
}
