package com.github.peter_kutak.test;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.peter_kutak.Id128;
import com.github.peter_kutak.SdId128Mapping;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import static org.junit.Assert.*;

/**
 * 
 */
public class Id128Test {

  /**
   * 
   */
  @Test
  public void testSdId128Type() {
    SdId128Mapping sd = SdId128Mapping.INSTANCE;

    SdId128Mapping.sd_id128 m = new SdId128Mapping.sd_id128();
    sd.sd_id128_get_machine(m);
//    System.out.println(m);
//    assertEquals("machine", "test", m.toString());
    SdId128Mapping.sd_id128 id= new SdId128Mapping.sd_id128();
    sd.sd_id128_get_boot(id);
//    System.out.println(id);
  }

  @Test
  public void appSpecific() {
    String APP_ID = "ce74fb5f-fa74-4968-979f-824c61695ecc";
    String machine_id = Id128.get_machine_id();
    String a = Id128.get_machine_app_specific(APP_ID);
    SdId128Mapping.sd_id128 b = Id128.hmac256(machine_id, APP_ID);
    assertEquals("app_specific vs hmac256", a, b.toString());
    //assertArrayEquals("app_specific vs hmac256", a.bytes, b.bytes);
  }
}
