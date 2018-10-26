import lombok.ToString;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ToString
public class Intent {
    private String name;
    private float confidence;
}
