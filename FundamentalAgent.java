import java.util.Random;

public class FundamentalAgent extends Agent{

    // ファンダメンタル価格
    private double pf;
    // 注文価格の最大値
    private static int PRICE_MAX = 100000;
	// エージェントの注文数
    private static int ORDER_NUM = 1;
    
    // 所持しているポジション
	// 買いポジション：+1
	// 売りポジション：-1
	private int position;
    // エージェントの買いと売りの判断
	private Flag.Trade tradeFlag;

    // パラメータクラス
    Parameter param;
    // 乱数
    Random rand;

	// 25/04/05 追加変数iを記録
	private int beforetime = 0;
	private int aftertime = 0;

    public FundamentalAgent(Parameter param, int agentId) {
        super(); // 親クラスのコンストラクタを呼び出す
        
        this.param = param;
        rand = new Random(param.getSeedForAgent(agentId + 10000)); // 重複を避ける
        // ファンダメンタル価格（パラメータクラスから取得）
        this.pf = param.getP_fund();
        // ポジション
        this.position = 0;
        // 初期状態では注文なし
        this.tradeFlag = Flag.Trade.NONE;
    }

    // 注文の発注
	public Flag.Trade order(Market market, int time, int i) {
		// 25/04/05 追加変数iを活用
		this.beforetime = this.aftertime;
		this.aftertime = time;

        // 直近の市場価格
		double marketPrice = market.getMarketPrice();
		// 最良買い気配値
		double bestBid = market.getBestBid();
		// 25/04/05 次点の最良気配値の値を取得する
		double secondbestBid = market.getSecondBestBid();
		// 最良売り気配値
		double bestAsk = market.getBestAsk();
		// 25/04/05 次点の最良気配値の値を取得する
		double secondbestAsk = market.getSecondBestAsk();

		// 25/04/05 1回目の注文の場合
		if(this.beforetime != this.aftertime) {
			// 株式を所有していない場合
			if(this.position == 0){
				// ファンダメンタル価格より最良買い気配値が高い場合は売り
				if(bestBid > this.pf){
					this.tradeFlag = Flag.Trade.SELL;
				} 
				// ファンダメンタル価格より最良売り気配値が低い場合は買い
				else if(bestAsk < this.pf){
					this.tradeFlag = Flag.Trade.BUY;
				} 
				// 同じ場合は注文なし
				else {
					this.tradeFlag = Flag.Trade.NONE;
				}
			} // 株式を所有していた場合
			else if(this.position == 1){		
				// 25/04/05 ファンダメンタル価格より、次点最良買い気配値が高い場合は売り
				if(secondbestBid > this.pf){
					this.tradeFlag = Flag.Trade.SELL;
				} 
				// ファンダメンタル価格より最良売り気配値が低い場合は注文なし
				else if(secondbestAsk < this.pf){
					this.tradeFlag = Flag.Trade.NONE;
				} 
				// 同じ場合は注文なし
				else {
					this.tradeFlag = Flag.Trade.NONE;
				}	
			} // 株式を空売りしていた場合
			else if(this.position == -1){		
				// ファンダメンタル価格より最良買い気配値が高い場合は注文なし
				if(secondbestBid > this.pf){
					this.tradeFlag = Flag.Trade.NONE;
				} 
				// 25/04/05 ファンダメンタル価格より、次点最良売り気配値が低い場合は買い
				else if(secondbestAsk < this.pf){
					this.tradeFlag = Flag.Trade.BUY;
				} 
				// 同じ場合は注文なし
				else {
					this.tradeFlag = Flag.Trade.NONE;
				}
			}
		} // 25/04/05 2回目の注文の場合
		else {
			// 株式を所有していない場合
			if(this.position == 0){
				// ファンダメンタル価格より最良買い気配値が高い場合は売り
				if(bestBid > this.pf){
					this.tradeFlag = Flag.Trade.SELL;
				} 
				// ファンダメンタル価格より最良売り気配値が低い場合は買い
				else if(bestAsk < this.pf){
					this.tradeFlag = Flag.Trade.BUY;
				} 
				// 同じ場合は注文なし
				else {
					this.tradeFlag = Flag.Trade.NONE;
				}
			} // 株式を所有していた場合
			else {
				this.tradeFlag = Flag.Trade.NONE;
			}
		}

        // 発注処理
		if(this.tradeFlag != Flag.Trade.NONE) {
			// 板形成期間中は注文しない
			if(time <= this.param.getT_bookMake()) {
				this.tradeFlag = Flag.Trade.NONE;
			}
			if(tradeFlag == Flag.Trade.NONE) {
				return this.tradeFlag; 
			}

			// 市場に注文を出す
			double orderPrice1;

			// 買い注文の場合
            if(this.tradeFlag == Flag.Trade.BUY) {
                // 1回目の注文 - 現在の最良売り気配値で成行注文
                orderPrice1 = market.getBestAsk(); // 最良売り気配値を取得
				Order order1 = new Order(this, time, orderPrice1, ORDER_NUM, this.tradeFlag);
			    market.addOrder(order1);
            } 
            // 売り注文の場合
            else if(this.tradeFlag == Flag.Trade.SELL) {
                // 1回目の注文 - 現在の最良買い気配値で成行注文
                orderPrice1 = market.getBestBid(); // 最新の最良買い気配値を取得
                Order order1 = new Order(this, time, orderPrice1, ORDER_NUM, this.tradeFlag);
			    market.addOrder(order1);
            } 
            // その他の場合（通常はこの分岐には入らない）
            else {
                // 1回目の注文
                Order order1 = new Order(this, time, this.pf, ORDER_NUM, this.tradeFlag);
			    market.addOrder(order1);
            }
		}

		return this.tradeFlag;
    }

    // 板形成期間の買いと売りの判断
	public Flag.Trade bookMakeOrderType(double orderPrice) {
		// 注文価格がファンダメンタル価格よりも高かった場合は売り注文
		if(orderPrice >= this.param.getP_fund()) {
			return Flag.Trade.SELL;
		}
		// 注文価格がファンダメンタル価格よりも低かった場合は買い注文
		else if(orderPrice < this.param.getP_fund()) {
			return Flag.Trade.BUY;
		}
		// ファンダメンタル価格と同じ場合（通常このケースは稀）
		else {
			return Flag.Trade.NONE;
		}
	}

    // ポジションの更新
	@Override
	public void updatePosition(Flag.Trade tradeFlag, int orderVol) {
		// 買い注文の場合：ポジションを増加
		if(tradeFlag == Flag.Trade.BUY) {
			position = position + orderVol;
		}
		// 売り注文の場合：ポジションを減少
		else if(tradeFlag == Flag.Trade.SELL) {
			position = position - orderVol;
		}
		// NONE の場合は何もしない
	}

    // getterメソッド群
    
    // 注文タイプを返す
	public Flag.Trade getTradeFlag() {
		return this.tradeFlag;
	}
	
	// 現在のポジションを返す
	public int getPosition() {
		return this.position;
	}
	
	// ファンダメンタル価格を返す
	public double getFundamentalPrice() {
		return this.pf;
	}
	
	// ファンダメンタル価格を設定する
	public void setFundamentalPrice(double newPrice) {
		this.pf = newPrice;
	}
}