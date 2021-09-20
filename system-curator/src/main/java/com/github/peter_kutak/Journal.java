package com.github.peter_kutak;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

import com.sun.jna.platform.linux.LibC;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;

/**
 * SystemD journal.
 *
 */
public class Journal implements AutoCloseable {

  //errno je makro a moc to nefunguje, 
  //namiesto toho treba funkcie ktore nastavuju errno deklarovat ako throws LastErrorException

  /**
   *
   */
  static String strerror(int e) {
    //LIBC = LibC.INSTANCE;
    //NativeLibrary LIBC = NativeLibrary.getInstance("c");
    //return LIBC.strerror(e);
    return "" + e;
  }

  /**
   * Premeni bytes prijate z journalu na string a odfiltruje prefix "FIELD=".
   */
  String decodeField(PointerByReference data, NativeLongByReference length, String field) {
    long l = length.getValue().longValue();
    if (l == 0) {
      return "";
    }
    ByteBuffer b = data.getValue().getByteBuffer(0L, l);
    String s = StandardCharsets.UTF_8.decode(b).toString();
    s = s.substring(field.length() + 1);
    return s;
  }

  /**
   * Premeni bytes prijate z journalu na string a rozdeli na dvojicu "FIELD"="VALUE".
   */
  Pair<String, String> decodeData(PointerByReference data, NativeLongByReference length) {
    long l = length.getValue().longValue();
    ByteBuffer b = data.getValue().getByteBuffer(0L, l);
    String s = StandardCharsets.UTF_8.decode(b).toString();
    //System.out.println("data = "+s);
    int a = s.indexOf('=');
    String field = s.substring(0, a);
    String value = s.substring(a + 1);
    return Pair.of(field, value);
  }

  /**
   *
   */
  public static String get_boot_id() {
    SdId128Mapping sd = SdId128Mapping.INSTANCE;
    SdId128Mapping.sd_id128 id = new SdId128Mapping.sd_id128();
    sd.sd_id128_get_boot(id);
    return id.toString();
  }

  /**
   *
   */
  public static String get_boot_app_specific(String app_id) {
    SdId128Mapping sd = SdId128Mapping.INSTANCE;
    SdId128Mapping.sd_id128 id = new SdId128Mapping.sd_id128();
    sd.sd_id128_get_boot_app_specific(new SdId128Mapping.sd_id128.ByValue(app_id), id);
    return id.toString();
  }

  /**
   *
   */
  public static String get_machine_id() {
    SdId128Mapping.sd_id128 id = new SdId128Mapping.sd_id128();
    SdId128Mapping.INSTANCE.sd_id128_get_machine(id);
    return id.toString();
  }

  /**
   *
   */
  public static String get_machine_app_specific(String app_id) {
    SdId128Mapping.sd_id128 id = new SdId128Mapping.sd_id128();
    SdId128Mapping.INSTANCE.sd_id128_get_machine_app_specific(new SdId128Mapping.sd_id128.ByValue(app_id), id);
    return id.toString();
  }

  SdJournalMapping sd = SdJournalMapping.INSTANCE;
  Pointer journal = Pointer.NULL;

  /**
   * Otvori journal.
   */
  public void open() {
    PointerByReference p = new PointerByReference();
    int r = sd.sd_journal_open(p, SdJournalMapping.SD_JOURNAL_LOCAL_ONLY);
    if (r >= 0) {
      journal = p.getValue();
    }
  }

  /**
   * Uzavrie journal.
   */
  public void close() {
    sd.sd_journal_close(journal);
    journal = Pointer.NULL;
  }

  /**
   * Zoznam vsetkych stlpcov v journale. 
   * Zoznam je usporiadany.
   */
  public List<String> enumerate_fields() {
    List<String> result = new LinkedList<String>();
    PointerByReference field = new PointerByReference();
    //SD_JOURNAL_FOREACH_FIELD(j, field)
    for (sd.sd_journal_restart_fields(journal);
        sd.sd_journal_enumerate_fields(journal, field) > 0; ) {
      result.add(field.getValue().getString(0, "UTF-8"));
    }
    result.sort(Comparator.naturalOrder());
    return result;
  }


  /**
   * Prida filter na riadky citane z journalu.
   */
  public int add_match(String data) {
    return sd.sd_journal_add_match(journal, data, new NativeLong(data.length()));
  }

  /**
   * Zrusi filter na riadky citane z journalu.
   */
  public void flush_matches() {
    sd.sd_journal_flush_matches(journal);
  }

  /**
   * Presunie kurzor na zaciatok journalu.
   * Kurzor je pred prvym zaznamom, takze pred citanim dat je potrebne volat next().
   */
  public int seek_head() {
    return sd.sd_journal_seek_head(journal);
  }

  /**
   * Presunie kurzor na dalsi riadok v jurnale.
   */
  public int next() {
    return sd.sd_journal_next(journal);
  }

  /**
   * Presunie kurzor na riadok identifikovny stringom.
   * Kurzor je pred zaznamom, takze pred citanim dat je potrebne volat next().
   */
  public int seek_cursor(String	cursor) {
    return sd.sd_journal_seek_cursor(journal, cursor);
  }

  /**
   * Ziska string identifikujuci aktualny riadok journalu.
   */
  public String get_cursor() {
    PointerByReference c = new PointerByReference();
    if (sd.sd_journal_get_cursor(journal, c) >= 0) {
      return c.getValue().getString(0, "UTF-8");
    } else {
      return null;
    }
  }

  /**
   * Read timestamp from the current journal entry.
   */
  public Instant get_realtime() {
    LongByReference usec = new LongByReference();
    sd.sd_journal_get_realtime_usec(journal, usec);
    long u = usec.getValue();
    return Instant.ofEpochSecond(u / 1000000, u % 1000000 * 1000);
  }

  /**
   * Zoznam dvojic FIELD,VALUE platnych poli aktualneho riadku, ktore obsahuju niake data.
   */
  public List<Pair<String, String>> enumerate_available_data(String cursor) {
    List<Pair<String, String>> result = new LinkedList();
    this.seek_cursor(cursor);
    this.next();
    PointerByReference data = new PointerByReference();
    NativeLongByReference l = new NativeLongByReference();
    //SD_JOURNAL_FOREACH_DATA(j, data, l)
    for (sd.sd_journal_restart_data(journal);
        sd.sd_journal_enumerate_available_data(journal, data, l) > 0; ) {
      result.add(decodeData(data, l));
    }
    return result;
  }

  /**
   * Zoznam dvojic FIELD,VALUE poli aktualneho riadku, vsetkych aj tie co su prazdne.
   */
  public List<Pair<String, String>> enum_data() {
    List<Pair<String, String>> result = new LinkedList();
    PointerByReference data = new PointerByReference();
    NativeLongByReference l = new NativeLongByReference();
    //SD_JOURNAL_FOREACH_DATA(j, data, l)
    for (sd.sd_journal_restart_data(journal);
        sd.sd_journal_enumerate_available_data(journal, data, l) > 0; ) {
      result.add(decodeData(data, l));
    }
    return result;
  }

  /**
   *
   */
  public String get_data(String field) {
    PointerByReference data = new PointerByReference();
    NativeLongByReference l = new NativeLongByReference();
    int d = sd.sd_journal_get_data(journal, field, data, l);
    return decodeField(data, l, field);
  }

  /**
   *
   */
  public List<String> query_unique(String field) {
    List<String> result = new LinkedList<String>();
    int r = sd.sd_journal_query_unique(journal, field);
    if (r < 0) {
      System.err.println("Failed to query journal: " + strerror(-r) + "\n");
      return result;
    }
    //SD_JOURNAL_FOREACH_UNIQUE(j, d, l)
    PointerByReference data = new PointerByReference();
    NativeLongByReference length = new NativeLongByReference();
    for (sd.sd_journal_restart_unique(journal);
        sd.sd_journal_enumerate_available_unique(journal, data, length) > 0; ) {
      result.add(decodeField(data, length, field));
    }
    return result;
  }

  public String getMachineHostname(String id) {
    String filter = "_MACHINE_ID=" + id;
    this.flush_matches();
    this.add_match(filter);
    this.seek_head();
    this.next();
    return this.get_data("_HOSTNAME");
  }

   public Instant getBootBegin(String id) {
    String filter = "_BOOT_ID=" + id;
    this.flush_matches();
    this.add_match(filter);
    this.seek_head();
    this.next();
    return this.get_realtime();
  }

  /**
   *
   */
  public static void foreach() {
    SdJournalMapping sdJournal = SdJournalMapping.INSTANCE;
    System.out.println(sdJournal);
    PointerByReference p = new PointerByReference();
    int r;

    r = sdJournal.sd_journal_open(p, SdJournalMapping.SD_JOURNAL_LOCAL_ONLY);
    if (r < 0) {
      System.err.println("Failed to open journal: " + strerror(-r) + "\n");
      return;
    }
    Pointer j = p.getValue();
    r = SdJournalMapping.INSTANCE.sd_journal_query_unique(j, "_SYSTEMD_USER_UNIT");
    if (r < 0) {
      System.err.println("Failed to query journal: " + strerror(-r) + "\n");
      return;
    }
    //const void *d;
    PointerByReference data = new PointerByReference();
    //size_t l;
    NativeLongByReference length = new NativeLongByReference();
    //SD_JOURNAL_FOREACH_UNIQUE(j, d, l)
    for (sdJournal.sd_journal_restart_unique(j);
        sdJournal.sd_journal_enumerate_available_unique(j, data, length) > 0; ) {
      System.out.println("" + data.getValue().getString(0));
    }
    sdJournal.sd_journal_close(j);
  }
}

