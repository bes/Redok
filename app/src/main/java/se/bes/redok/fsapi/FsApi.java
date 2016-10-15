package se.bes.redok.fsapi;

import lombok.RequiredArgsConstructor;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface FsApi {
    @RequiredArgsConstructor
    public enum Mode {
        RADIO(0), SPOTIFY(1);

        public final int mode;
    }

    @GET("CREATE_SESSION")
    Call<String> createSession();

    /**
     * Used to set the mode of the device
     * @param pin The pin code (usually "1234")
     * @param value The mode as an int, see {@link Mode} for possible values
     */
    @GET("SET/netRemote.sys.mode")
    Call<FsApiResponse> setSysMode(@Query("pin") String pin, @Query("value") int value);

    @GET("GET/netRemote.sys.audio.volume")
    Call<FsApiResponse> getSysAudioVolume(@Query("pin") String pin);

    @GET("SET/netRemote.sys.audio.volume")
    Call<FsApiResponse> setSysAudioVolume(@Query("pin") String pin, @Query("value") int volume);
}
