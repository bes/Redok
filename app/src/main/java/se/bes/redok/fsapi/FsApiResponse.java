package se.bes.redok.fsapi;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Root(name = "fsapiResponse")
public class FsApiResponse {
    @Element(name = "status")
    private String status;

    @Path("value")
    @Element(name = "u8", required = false)
    private Integer u8;
}
