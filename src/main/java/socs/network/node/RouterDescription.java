package socs.network.node;

import socs.network.util.Utility;


public class RouterDescription {

  //used to socket communication
  private String processIPAddress;
  private short processPortNumber;
  //used to identify the router in the simulated network space
  private String simulatedIPAddress;
  //status of the router
  private RouterStatus status;

  private RouterDescription(RouterDescriptionBuilder b) {
    this.processIPAddress = b.processIPAddress;
    this.simulatedIPAddress = b.simulatedIPAddress;
    this.processPortNumber = b.processPortNumber;
    this.status = b.status;
  }

  public RouterDescription() {
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof RouterDescription)) //type checking
      return false;

    RouterDescription lhs = (RouterDescription) obj;

    if (lhs.getSimulatedIPAddress() != getSimulatedIPAddress()
            || lhs.getProcessPortNumber() != getProcessPortNumber()
            || lhs.getStatus() != getStatus()
            || lhs.getProcessIPAddress() != getProcessIPAddress())
      return false;

    return true;

  }

  public String getProcessIPAddress() {
    return processIPAddress;
  }

  public short getProcessPortNumber() {
    return processPortNumber;
  }

  public String getSimulatedIPAddress() {
    return simulatedIPAddress;
  }

  public RouterStatus getStatus() {
    return status;
  }


  public void setProcessIPAddress(String processIPAddress) {

    if (processIPAddress == null || !Utility.validateIP(processIPAddress))
      throw new IllegalArgumentException("Invalid argument");


    this.processIPAddress = processIPAddress;
  }

  public void setProcessPortNumber(short processPortNumber) {

    if (processPortNumber < 0 || processPortNumber > Short.MAX_VALUE)
      throw new IllegalArgumentException("Invalid argument, out of range");

    this.processPortNumber = processPortNumber;
  }

  public void setSimulatedIPAddress(String simulatedIPAddress) {

    if (simulatedIPAddress == null || !Utility.validateIP(simulatedIPAddress))
      throw new IllegalArgumentException("Invalid argument");

    this.simulatedIPAddress = simulatedIPAddress;
  }

  public void setStatus(RouterStatus status) {

    if (status == null)
      throw new IllegalArgumentException("Argument is null");

    this.status = status;
  }

  public static class RouterDescriptionBuilder {

    //used to socket communication
    private String processIPAddress;
    private short processPortNumber;
    //used to identify the router in the simulated network space
    private  String simulatedIPAddress;
    //status of the router
    private RouterStatus status;


    public RouterDescriptionBuilder processIPAddress(String processIPAddress) {
      this.processIPAddress = processIPAddress;
      return this;
    }

    public RouterDescriptionBuilder processPortNumber(short processPortNumber) {
      this.processPortNumber = processPortNumber;
      return this;
    }

    public RouterDescriptionBuilder simulatedIPAddress(String simulatedIPAddress) {
      this.simulatedIPAddress = simulatedIPAddress;
      return this;
    }

    public RouterDescriptionBuilder INIT() {
      this.status = RouterStatus.INIT;
      return this;
    }

    public RouterDescriptionBuilder DEFAULT() {
      this.status = RouterStatus.DEFAULT;
      return this;
    }

    public RouterDescriptionBuilder TWO_WAY() {
      this.status = RouterStatus.TWO_WAY;
      return this;
    }

    public RouterDescription build() {
      return new RouterDescription(this);
    }




  }

}
