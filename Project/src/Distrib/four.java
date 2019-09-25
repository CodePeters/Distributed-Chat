package Distrib;


class four<T, U, V, X> {

    private final T first;
    private final U second;
    private final V third;
    private final X fourth;

    four(T first, U second, V third, X fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    T getFirst() { return first; }
    U getSecond() { return second; }
    V getThird() { return third; }
    X getFourth() { return fourth; }
}