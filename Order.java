public class Order {

	// 注文者
	private Agent orderAgent;
	// 注文時刻
	private int orderTime;
	// 注文価格
	private double orderPrice;
	// 注文数量
	private int orderNum;
	// 買い注文か売り注文かのタイプ
	private Flag.Trade tradeFlag;

	// Orderクラスのコンストラクタ
	public Order(Agent agent, int time, double price, int num, Flag.Trade tradeFlag) {
		this.orderAgent = agent;
		this.orderTime = time;
		this.orderPrice = price;
		this.orderNum = num;
		this.tradeFlag = tradeFlag;
	}

	// 約定した板の注文数量を減らす
	public void removeOrderNum(int num) {
		this.orderNum = this.orderNum - num;
	}

	// 注文者
	public Agent getOrderAgent() {
		return this.orderAgent;
	}
	//注文時刻
	public int getOrderTime() {
		return this.orderTime;
	}
	// 注文価格
	public double getOrderPrice() {
		return this.orderPrice;
	}
	// 注文数量
	public int getOrderNum() {
		return this.orderNum;
	}
	// 買い注文か売り注文かのタイプ
	public Flag.Trade getTradeFlag() {
		return this.tradeFlag;
	}
}
