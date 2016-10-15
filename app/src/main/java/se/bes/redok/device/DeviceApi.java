package se.bes.redok.device;

import retrofit2.Call;
import retrofit2.http.GET;

public interface DeviceApi {
    /**
     * When you have discovered a device use the device endpoint to get information about the
     * device (NetRemote)
     */
    @GET(".")
    Call<NetRemote> getDevice();
}
