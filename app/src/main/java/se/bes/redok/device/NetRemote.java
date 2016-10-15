package se.bes.redok.device;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Root(name = "netRemote")
public class NetRemote {
    @Element(name = "friendlyName")
    private String friendlyName;

    @Element(name = "version")
    private String version;

    @Element(name = "webfsapi")
    private String webfsapi;
}
