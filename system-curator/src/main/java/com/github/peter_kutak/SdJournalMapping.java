package com.github.peter_kutak;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
 
/**
 *
 */
public interface SdJournalMapping extends Library {
  String NAME = "systemd";
  SdJournalMapping INSTANCE = Native.load(NAME, SdJournalMapping.class);

  /* Open flags */
  public static final int SD_JOURNAL_LOCAL_ONLY                = 1 << 0;
  public static final int SD_JOURNAL_RUNTIME_ONLY              = 1 << 1;
  public static final int SD_JOURNAL_SYSTEM                    = 1 << 2;
  public static final int SD_JOURNAL_CURRENT_USER              = 1 << 3;
  public static final int SD_JOURNAL_OS_ROOT                   = 1 << 4;
  /* Show all namespaces, not just the default or specified one */
  public static final int SD_JOURNAL_ALL_NAMESPACES            = 1 << 5;
  /* Show default namespace in addition to specified one */
  public static final int SD_JOURNAL_INCLUDE_DEFAULT_NAMESPACE = 1 << 6;
  /* old name */
  public static final int SD_JOURNAL_SYSTEM_ONLY = SD_JOURNAL_SYSTEM;

  int sd_journal_open(PointerByReference ret, int flags) throws LastErrorException;
  void sd_journal_close(Pointer j);
  int sd_journal_query_unique(Pointer j, String field);
  int sd_journal_enumerate_available_unique(Pointer j, PointerByReference data,
      NativeLongByReference l);
  void sd_journal_restart_unique(Pointer j);

  int sd_journal_seek_head(Pointer j);
  int sd_journal_next(Pointer j);
  int sd_journal_seek_cursor(Pointer j, String cursor);

  /**
   * Ak je size 0, data sa beru ako null-terminated string.
   */
  int sd_journal_add_match(Pointer j, String data, NativeLong size);
  int sd_journal_add_disjunction(Pointer j);
  int sd_journal_add_conjunction(Pointer j);
  void sd_journal_flush_matches(Pointer j);

  int sd_journal_get_data(Pointer j, String field, PointerByReference data,
      NativeLongByReference l);
  int sd_journal_enumerate_available_data(Pointer j,
 	    PointerByReference data,
      NativeLongByReference length);
  void sd_journal_restart_data(Pointer j);
  int sd_journal_get_realtime_usec(Pointer j, LongByReference usec);

  int sd_journal_get_cursor(Pointer j, PointerByReference cursor);

  int sd_journal_test_cursor(Pointer j, String cursor);

  int sd_journal_enumerate_fields(Pointer j, PointerByReference field);
  void sd_journal_restart_fields(Pointer j);

}

