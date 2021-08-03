package haslab.eo;

class Interval {
	long from;
	long to;

	public Interval(long from, long to) {
		this.from = from;
		this.to = to;
	}

	public void append(long newTo) {
		to = newTo;
	}

	public void append() {
		to += 1;
	}

	public long dequeue() {
		from += 1;
		return from - 1;
	}

	public long size() {
		return to - from;
	}

	public long first() {
		return from;
	}
}