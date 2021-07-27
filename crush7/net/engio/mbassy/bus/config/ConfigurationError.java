package net.engio.mbassy.bus.config;

public class ConfigurationError extends RuntimeException {
  private String message;
  
  private ConfigurationError(String message) {
    this.message = message;
  }
  
  public static ConfigurationError MissingFeature(Class<? extends Feature> featureType) {
    return new ConfigurationError("The expected feature " + featureType + " was missing. Use addFeature() in IBusConfiguration to add features.");
  }
  
  public String toString() {
    return this.message;
  }
}
