package org.zoxweb.server.util;

import java.io.IOException;
//import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.io.IOUtil;

public class YamlJsonUtilTest {


  public String getYamlData() throws IOException
  {
    return IOUtil.inputStreamToString(getClass().getClassLoader().getResourceAsStream("TestData.yaml"), true);
  }

  public String getJsonData() throws IOException
  {
    return IOUtil.inputStreamToString(getClass().getClassLoader().getResourceAsStream("network-config.json"), true);
  }

  @Test
  public void convertToJson() throws IOException {
    System.out.println(YamlJsonUtil.yamlToJson(getYamlData()));
  }

  @Test
  public void convertToYaml() throws IOException {
    String yamlString = getYamlData();
    String jsonData = YamlJsonUtil.yamlToJson(yamlString);
    System.out.println(YamlJsonUtil.jsonToYaml(jsonData));
  }

  @Test
  public void convertJsonToYaml() throws IOException {
    System.out.println(YamlJsonUtil.jsonToYaml(getJsonData()));
  }



}
