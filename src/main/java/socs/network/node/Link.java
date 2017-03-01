package socs.network.node;

/**
 * Invariants :
 * Links are symmetric
 */
public class Link {

    private final RouterDescription router1;
    private final RouterDescription router2;
    private short weight;

    public Link(RouterDescription r1, RouterDescription r2, short weight) {
        router1 = r1;
        router2 = r2;
        this.weight = weight;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Link)) // type checking
            return false;

        Link lhs = (Link) obj;
        if (lhs.getWeight() != getWeight()) return false;

        return (lhs.getRouter1().equals(router1) && lhs.getRouter2().equals(router2)) || (lhs.getRouter1().equals(router2) && lhs.getRouter2().equals(router1));


    }

    public RouterDescription getRouter1() {
        return router1;
    }

    public RouterDescription getRouter2() {
        return router2;
    }

    public short getWeight() {
        return weight;
    }

    public void setWeight(short weight) {
        this.weight = weight;
    }

    /**
     * Utility function to get the other end.
     *
     * @param simulatedIP caller IP
     * @return router description of the endpoint router.
     */
    public RouterDescription getOtherEnd(String simulatedIP) {
        if (router1.getSimulatedIPAddress().equals(simulatedIP))
            return router2;
        else if (router2.getSimulatedIPAddress().equals(simulatedIP))
            return router1;

        return null;
    }




}
