package edu.stanford.nlp.time;

import de.jollyday.HolidayManager;
import de.jollyday.config.Configuration;
import de.jollyday.config.Holidays;
// import de.jollyday.configuration.ConfigurationProvider;
import de.jollyday.impl.XMLManager;
import edu.stanford.nlp.ling.tokensregex.Env;
import edu.stanford.nlp.net.ClasspathURLStreamHandler;
import edu.stanford.nlp.util.CollectionValuedMap;
import edu.stanford.nlp.util.Generics;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Partial;

import java.lang.reflect.Method;
// import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Wrapper around jollyday library so we can hook in holiday
 * configurations from jollyday with SUTime.
 *
 * @author Angel Chang
 */
public class JollyDayHolidays implements Env.Binder {
  HolidayManager holidayManager;
  //CollectionValuedMap<String, JollyHoliday> holidays;
  Map<String, JollyHoliday> holidays;
  String varPrefix = "JH_";

  @Override
  public void init(String prefix, Properties props) {
    String country = props.getProperty(prefix + "country", "sutime");
    varPrefix = props.getProperty(prefix + "prefix", varPrefix);
    Properties managerProps = new Properties();
    managerProps.setProperty("manager.impl", "edu.stanford.nlp.time.JollyDayHolidays$MyXMLManager");
    try {
      holidayManager = HolidayManager.getInstance(new URL("classpath", null, 0, "edu/stanford/nlp/models/sutime/jollyday/Holidays_sutime.xml", new ClasspathURLStreamHandler()), managerProps);
    } catch (java.net.MalformedURLException e) {
      throw new RuntimeException(e);
    }
    System.err.println("Initializing JollyDayHoliday for " + country);
    if (!(holidayManager instanceof MyXMLManager)) {
      throw new AssertionError("Did not get back JollyDayHolidays$MyXMLManager");
    }
    Configuration config = ((MyXMLManager) holidayManager).getConfiguration();
    holidays = getAllHolidaysMap(config);
  }

  public void bind(Env env) {
    if (holidays != null) {
      for (String s:holidays.keySet()) {
        JollyHoliday jh = holidays.get(s);
        env.bind(varPrefix + s, jh);
      }
    }
  }

  public Map<String, JollyHoliday> getAllHolidaysMap(Set<de.jollyday.config.Holiday> allHolidays)
  {
    Map<String, JollyHoliday> map = Generics.newHashMap();
    for (de.jollyday.config.Holiday h:allHolidays) {
      String descKey = h.getDescriptionPropertiesKey();
      if (descKey != null) {
        descKey = descKey.replaceAll(".*\\.","");
        JollyHoliday jh = new JollyHoliday(descKey, holidayManager, h);
        map.put(jh.label, jh);
      }
    }
    return map;
  }

  public Map<String, JollyHoliday> getAllHolidaysMap(Configuration config)
  {
    Set<de.jollyday.config.Holiday> s = getAllHolidays(config);
    return getAllHolidaysMap(s);
  }

  public CollectionValuedMap<String, JollyHoliday> getAllHolidaysCVMap(Set<de.jollyday.config.Holiday> allHolidays)
  {
    CollectionValuedMap<String, JollyHoliday> map = new CollectionValuedMap<String, JollyHoliday>();
    for (de.jollyday.config.Holiday h:allHolidays) {
      String descKey = h.getDescriptionPropertiesKey();
      if (descKey != null) {
        descKey = descKey.replaceAll(".*\\.","");
        JollyHoliday jh = new JollyHoliday(descKey, holidayManager, h);
        map.add(jh.label, jh);
      }
    }
    return map;
  }

  public CollectionValuedMap<String, JollyHoliday> getAllHolidaysCVMap(Configuration config)
  {
    Set<de.jollyday.config.Holiday> s = getAllHolidays(config);
    return getAllHolidaysCVMap(s);
  }

  public static void getAllHolidays(Holidays holidays, Set<de.jollyday.config.Holiday> allHolidays)
  {
    for (Method m : holidays.getClass().getMethods()) {
      if (isGetter(m) && m.getReturnType() == List.class) {
        try {
          List l = (List) m.invoke(holidays);
          allHolidays.addAll(l);
        } catch (Exception e) {
          throw new RuntimeException("Cannot create set of holidays.", e);
        }
      }
    }
  }

  public static void getAllHolidays(Configuration config, Set<de.jollyday.config.Holiday> allHolidays)
  {
    Holidays holidays = config.getHolidays();
    getAllHolidays(holidays, allHolidays);
    List<Configuration> subConfigs = config.getSubConfigurations();
    for (Configuration c:subConfigs) {
      getAllHolidays(c, allHolidays);
    }
  }

  public static Set<de.jollyday.config.Holiday> getAllHolidays(Configuration config)
  {
    Set<de.jollyday.config.Holiday> allHolidays = Generics.newHashSet();
    getAllHolidays(config, allHolidays);
    return allHolidays;
  }

  private static boolean isGetter(Method method) {
    return method.getName().startsWith("get")
            && method.getParameterTypes().length == 0
            && !void.class.equals(method.getReturnType());
  }

  public static class MyXMLManager extends XMLManager {
    public Configuration getConfiguration() {
      return configuration;
    }
  }

  public static class JollyHoliday extends SUTime.Time {
    HolidayManager holidayManager;
    de.jollyday.config.Holiday base;
    String label;

    public JollyHoliday(String label, HolidayManager holidayManager, de.jollyday.config.Holiday base) {
      this.label = label;
      this.holidayManager = holidayManager;
      this.base = base;
    }
    public JollyHoliday() {}

    public String toFormattedString(int flags) {
      if (getTimeLabel() != null) {
        return getTimeLabel();
      }
      if ((flags & SUTime.FORMAT_ISO) != 0) {
        return null;
      }
      return label;
    }

    public boolean isGrounded()  { return false; }
    public SUTime.Time getTime() { return this; }
    // TODO: compute duration/range => uncertainty of this time
    public SUTime.Duration getDuration() { return SUTime.DURATION_NONE; }
    public SUTime.Range getRange(int flags, SUTime.Duration granularity) { return new SUTime.Range(this,this); }
    public String toISOString() { return base.toString(); }
    public SUTime.Time intersect(SUTime.Time t) {
      SUTime.Time resolved = resolve(t, 0);
      if (resolved != this) {
        return resolved.intersect(t);
      } else {
        return super.intersect(t);
      }
    }
    public SUTime.Time resolve(SUTime.Time t, int flags) {
      Partial p = (t != null)? t.getJodaTimePartial():null;
      if (p != null) {
        if (JodaTimeUtils.hasField(p, DateTimeFieldType.year())) {
          int year = p.get(DateTimeFieldType.year());
          // TODO: If we knew location of article, can use that information to resolve holidays better
          Set<de.jollyday.Holiday> holidays = holidayManager.getHolidays(year);
          // Try to find this holiday
          for (de.jollyday.Holiday h:holidays) {
            if (h.getPropertiesKey().equals(base.getDescriptionPropertiesKey())) {
              return new SUTime.PartialTime(this, new Partial(h.getDate()));
            }
          }
        }
      }
      return this;
    }

    public SUTime.Time add(SUTime.Duration offset) {
      return new SUTime.RelativeTime(this, SUTime.TemporalOp.OFFSET, offset);
    }
  }
}
