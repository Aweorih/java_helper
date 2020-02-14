
import java.util.ArrayList;
import java.util.List;

// can encode a polyline string with a precision of 6 decimals to PolygonPoints
// can decode PolygonPoints to a polyline string with a precision of 6 decimals
public class PolylineHelper {

  // char area goes from 0x0 to 0x3e
  // if char is in first half we reached the end of current point part
  // for having a valid string we add 63
  // -> so in our string we have bytes of range 0x3F - 0x7e
  public static String encode(List<PolygonPoint> points) {

    int           precision = 6;
    long          factor    = (long) Math.pow(10, precision);
    long          lastLat   = 0;
    long          lastLon   = 0;
    StringBuilder builder   = new StringBuilder();

    for (PolygonPoint point : points) {

      long lat = (long) ((factor * point.lat) - lastLat) << 1;
      long lon = (long) ((factor * point.lon) - lastLon) << 1;

      if (lat < 0) {
        lat = ~(lat);
      }

      if (lon < 0) {
        lon = ~(lon);
      }

      if (lat > 0) {
        while (lat > 0) {
          // last char must be uppercase
          int  additional = ((lat >> 5) > 0) ? 32 : 0;
          char c          = (char) ((lat & 0x1f) + 63 + additional);
          builder.append(c);
          lat >>= 5;
        }
      } else {
        builder.append('?');
      }

      if (lon > 0) {
        while (lon > 0) {
          // last char must be uppercase
          int additional = ((lon >> 5) > 0) ? 32 : 0;
          builder.append((char) ((lon & 0x1f) + 63 + additional));
          lon >>= 5;
        }
      } else {
        builder.append('?');
      }

      lastLat = (long) (factor * point.lat);
      lastLon = (long) (factor * point.lon);
    }

    return builder.toString();
  }

  public static List<PolygonPoint> decode(String polyline) {

    int                precision   = 6;
    int                index       = 0;
    int                lat         = 0;
    int                lng         = 0;
    List<PolygonPoint> coordinates = new ArrayList<>();
    double             factor      = Math.pow(10, precision);

    // Coordinates have variable length when encoded, so just keep
    // track of whether we've hit the end of the string. In each
    // loop iteration, a single coordinate is decoded.
    while (index < polyline.length()) {

      int shift  = 0;
      int result = 0;
      int c;

      do {
        c = polyline.charAt(index++) - 63;
        result |= (c & 0x1f) << shift;
        shift += 5;
        // last char is uppercase
      } while (c >= 0x20);

      lat += ((result & 1) == 1 ? ~(result >> 1) : (result >> 1));

      shift = result = 0;

      if (index < polyline.length()) {
        do {
          c = polyline.charAt(index++) - 63;
          result |= (c & 0x1f) << shift;
          shift += 5;
          // last char is uppercase
        } while (c >= 0x20 && index < polyline.length());
      }

      lng += ((result & 1) == 1 ? ~(result >> 1) : (result >> 1));

      coordinates.add(new PolygonPoint(lat / factor, lng / factor));
    }

    return coordinates;
  }

  private PolylineHelper() {
    throw new AssertionError();
  } // never

  public static class PolygonPoint {

    public final double lat;
    public final double lon;

    public PolygonPoint(double lat, double lon) {
      this.lat = lat;
      this.lon = lon;
    }

    public boolean isThis(PolygonPoint that) {
      return Double.compare(that.lat, lat) == 0 &&
             Double.compare(that.lon, lon) == 0;
    }

    @Override
    public String toString() {
      return String.format("%s %s", lat, lon);
    }
  }
}
