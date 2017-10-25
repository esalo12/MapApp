
package Modules;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;


public interface RuteFinnerListener {
    void finnRuteResultat(List<Rute> rute, List<LatLng> punkter);
}