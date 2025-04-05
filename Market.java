import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Market {
	// エージェントタイプごとの収益追跡用変数
	private double fundamentalAgentTotalBuy = 0;
	private double fundamentalAgentTotalSell = 0;
	private double technicalAgentTotalBuy = 0;
	private double technicalAgentTotalSell = 0;
	private int fundamentalAgentBuyCount = 0;
	private int fundamentalAgentSellCount = 0;
	private int technicalAgentBuyCount = 0;
	private int technicalAgentSellCount = 0;
	// 全買い注文のリスト
	private List<Order> buyBook;
	// 全売り注文のリスト
	private List<Order> sellBook;
	// 直近の市場価格
	private double marketPrice;
	// 過去の市場価格の配列
	private double[] marketPriceArray;
	// １日の市場価格の最大値
	private double marketPriceMax;
	// １日の市場価格の最小値
	private double marketPriceMin;
	// 出来高
	private double volume;
	// １日あたりの出来高
	private double dayVolume;
	// Depthとして参照する幅
	double tickDepth;
	// Depthからティック何個分
	int TICK_DEPTH = 50;

//以下追加実装：取得データの宣言
	//買いの取引価格
	private double execBuyOrderPrice;
	//売りの取引価格
	private double execSellOrderPrice;
	// 取引が成立した、買いの指値注文の注文主エージェント情報の更新
	private String buyOrderAgent;
	// 取引が成立した、売りの指値注文の注文主エージェント情報の更新
	private String sellOrderAgent;
	//最良買い気配値の注文主エージェント情報
	private String bestBidAgent;
	//最良売り気配値の注文主エージェント情報
	private String bestAskAgent;
	//HFTエージェントの注文者情報
	private static String HFTAGENT = "H";
	// ノーマルエージェントの注文者情報
	private static String NORMAL = "N";
	// 注文者無しの注文者情報
	private static String NOTHING = "X";
	// HFTエージェントの買い注文価格
	private double hftBuyOrderPrice;
	// HFTエージェントの売り注文価格
	private double hftSellOrderPrice;
	// HFTのポジション
	private int hftPosition;
	// 取引が成立した、買いの成行注文の注文主エージェント情報の更新
	private String buyNewOrderAgent;
	// 取引が成立した、売りの成行注文の注文主エージェント情報の更新
	private String sellNewOrderAgent;
	// ターン数
	//private int turnCount;
	// HFTエージェントの成行注文発生のフラグ
	//修正：成行注文のフラグをString型からboolean型に変更
	private boolean nariyuki;

	//21/6/2
	private int hft_NAV ;
	
	
// 以上

	// 注文有効期間
	private int t_c;
	// パラメータクラス
	Parameter param;
	
	// 2024/07/17
	// 成行注文の出来高
	private double instantVolume = 0;
	// 指値注文の出来高
	private double limitVolume = 0;
	
	// 2024/07/24
	// 成行注文から指値注文に変更した出来高
	private double InstantToLimitVolume = 0;
	// 指値注文から成行注文に変更した出来高
	private double LimitToInstantVolume = 0;
	
	// コンストラクタ
	public Market(Parameter param) {
		// 買い板の初期化
		buyBook = new ArrayList<Order>();
		//売り板の初期化
		sellBook = new ArrayList<Order>();
		// パラメータクラス
		this.param = param;
		// 市場価格の初期化
		marketPriceArray = new double[param.getT_end() + 1];
		//市場価格をシミュレーション開始価格10000に
		this.marketPrice = param.getP_start();
		//市場価格のリストに現在の市場価格を追加
		marketPriceArray[0] = this.marketPrice;
		// 市場価格の最大値を初期化(10000に設定)
		this.marketPriceMax = param.getP_start();
		// 市場価格の最小値を初期化(10000に設定)
		this.marketPriceMin = param.getP_start();
		// 出来高の初期化
		this.volume = 0;
		// 1日あたりの出来高の初期化
		this.dayVolume = 0;
		// Depth計算の参照する板の幅の設定 tickDepthは50
		this.tickDepth = this.TICK_DEPTH;
		// 注文有効期間の設定 20000期
		this.t_c = param.getT_c();

//以下追加実装：HFTポジションの初期化
		this.hftPosition=0;
		//毎期ごとの期数の初期化
		//this.turnCount = 0;
		
		// 21/6/5
		this.hft_NAV = 0 ;
		
//以上

	}

	// 注文有効期間を経過した注文の取り消し
	public void removeOrder(int nowTime) {
		//買いの注文板の中から　現在時刻-注文時刻が注文有効期間を超えたものremoveで削除
		buyBook.removeIf(buy -> (nowTime - buy.getOrderTime()) > this.t_c);
		//売りの注文板の中から　現在時刻-注文時刻が注文有効期間を超えたものremoveで削除
		sellBook.removeIf(sell -> (nowTime - sell.getOrderTime()) > this.t_c);
	}

	// 板に注文を追加(HFTから呼ばれるのでこのメソッドはHFT.javaに入れた方がいいかも）
	public void addOrder(Order order) {
		// 注文を買いか売りの板に追加
		//オーダータイプが買いなら
		if(order.getTradeFlag() == Flag.Trade.BUY) {

//以下追加実装:HFTの買い注文価格の取得
//			   HFTの買いの成行注文発生のフラグをつける処理
			if(order.getOrderAgent() instanceof HFT){
				this.hftBuyOrderPrice=order.getOrderPrice();
				// 最良売り気配値を取得
				double bestSell = this.getBestAsk();
				// HFTの買い注文価格が最良売り気配値を上回っていたら成行注文発生のフラグをつける
				if(this.hftBuyOrderPrice>=bestSell){
					this.nariyuki = true;
				}else{
					this.nariyuki = false;
				}
			}
//以上

			//買いの注文板に注文を追加
			buyBook.add(order);
			// 買いの板を降順にソート
			//一番高く買う注文を一番上に
			Comparator<Order> comparator =
					Comparator.comparing(Order::getOrderPrice).reversed().thenComparing(Order::getOrderTime);
			buyBook.sort(comparator);
		}
		//オーダータイプが売りなら
		else if(order.getTradeFlag() == Flag.Trade.SELL) {

// 以下追加実装：HFTの売り注文価格の取得
//				 HFTの売りの成行注文発生のフラグをつける処理
			if(order.getOrderAgent() instanceof HFT){
				this.hftSellOrderPrice=order.getOrderPrice();
				// 最良買い気配値を取得
				double bestBuy = this.getBestBid();
				//HFTの売り注文価格が最良買い気配値を下回っていたら成行注文発生のフラグをつける
				if(this.hftSellOrderPrice<=bestBuy){
					this.nariyuki = true;
				}else{
					this.nariyuki = false;
				}
			}
// 以上

			//売りの注文板に注文を追加
			sellBook.add(order);
			// 売りの板を昇順にソート
			//一番安く売る注文を一番上に
			Comparator<Order> comparator =
					Comparator.comparing(Order::getOrderPrice).thenComparing(Order::getOrderTime);
			sellBook.sort(comparator);
		}
	}


// 以下追加実装：取引が成立した、注文主エージェント情報の取得
	// 板の注文成立を確認
	public void checkOrderBook(Flag.Trade tradeFlag) {
		// 最良気配値
		//ソートした注文板の一番上にある注文を buyに
		Order buy = buyBook.get(0);
		//ソートした注文板の一番上にある注文を sellに
		Order sell = sellBook.get(0);
		//一番高い買いを注文価格を設定
		double buyPrice = buy.getOrderPrice();
		//一番安い売りを注文価格を設定
		double sellPrice = sell.getOrderPrice();
		//volを0に初期化
		int vol = 0;

		// 買い注文よりも安い売り注文があった場合成立
		//当期にて発注されたNAの注文が買い　かつ　買いの価格が売りの価格よりも大きいなら
		if(tradeFlag == Flag.Trade.BUY && buyPrice >= sellPrice) {

			// 注文価格を取引注文価格に
			this.execBuyOrderPrice=buyPrice;
			this.execSellOrderPrice=sellPrice;

			//売りのエージェントがHFTなら
			if(sell.getOrderAgent() instanceof HFT){
				//HFTの注文が売りの成り行き注文なら
				if(this.nariyuki==true){
					//買い指値
					this.buyOrderAgent = NORMAL;
					//売り指値
					this.sellOrderAgent = NOTHING;
					//買い成行
					this.buyNewOrderAgent = NOTHING;
					//売り成行
					this.sellNewOrderAgent = HFTAGENT;

					//指値が「買い」の場合
					// 直近の市場価格を一番高い買いの価格に
					this.marketPrice = buyPrice;
					// 約定時の処理を行い，出来高をvolに加算
					vol += execOrder(sell, buyBook);

				}else{
					//買い指値
					this.buyOrderAgent = NOTHING;
					//売り指値
					this.sellOrderAgent = HFTAGENT;
					//買い成行
					this.buyNewOrderAgent = NORMAL;
					//売り成行
					this.sellNewOrderAgent = NOTHING;

					//指値が「売り」の場合
					// 直近の市場価格を指値注文の価格に
					this.marketPrice = sellPrice;
					// 約定時の処理を行い、成行が左、指値が右
					vol += execOrder(buy, sellBook);

				}
				//HFTポジション更新
				this.hftPosition+=-1;
			}
			//売りのエージェントがNAなら
			else if(sell.getOrderAgent() instanceof NormalAgent || sell.getOrderAgent() instanceof FundamentalAgent || sell.getOrderAgent() instanceof TechnicalAgent){
				//買いのエージェントがHFTなら
				if(buy.getOrderAgent() instanceof HFT){
					//買い指値
					this.buyOrderAgent = NOTHING;
					//売り指値
					this.sellOrderAgent = NORMAL;
					//買い成行
					this.buyNewOrderAgent = HFTAGENT;
					//売り成行
					this.sellNewOrderAgent = NOTHING;
					//HFTポジション更新
					this.hftPosition+=1;
				}
				//買いのエージェントがNAなら
				else if(buy.getOrderAgent() instanceof NormalAgent || buy.getOrderAgent() instanceof FundamentalAgent || buy.getOrderAgent() instanceof TechnicalAgent){
					//買い指値
					this.buyOrderAgent = NOTHING;
					//売り指値
					this.sellOrderAgent = NORMAL;
					//買い成行
					this.buyNewOrderAgent = NORMAL;
					//売り成行
					this.sellNewOrderAgent = NOTHING;
				}
				//指値が「売り」の場合
				// 直近の市場価格を指値注文の価格に
				this.marketPrice = sellPrice;
				// 約定時の処理を行い、成行が左、指値が右
				vol += execOrder(buy, sellBook);
			}
			// １日あたりの出来高にvolを加算
			this.dayVolume += vol;
			// 出来高にvolを加算
			this.volume += vol;
			
			// エージェントタイプごとの取引額を集計
			// 買い注文の集計
			if (buy.getOrderAgent() instanceof FundamentalAgent) {
				fundamentalAgentTotalBuy += this.marketPrice * vol;
				fundamentalAgentBuyCount++;
			} else if (buy.getOrderAgent() instanceof TechnicalAgent) {
				technicalAgentTotalBuy += this.marketPrice * vol;
				technicalAgentBuyCount++;
			}
			
			// 売り注文の集計
			if (sell.getOrderAgent() instanceof FundamentalAgent) {
				fundamentalAgentTotalSell += this.marketPrice * vol;
				fundamentalAgentSellCount++;
			} else if (sell.getOrderAgent() instanceof TechnicalAgent) {
				technicalAgentTotalSell += this.marketPrice * vol;
				technicalAgentSellCount++;
			}

			// 21/6/2 HFTのNAV計算
			if (this.buyOrderAgent == HFTAGENT || this.buyNewOrderAgent == HFTAGENT) {
				this.hft_NAV += this.marketPrice ;
			}
			if (this.sellOrderAgent == HFTAGENT || this.sellNewOrderAgent == HFTAGENT) {
				this.hft_NAV -= this.marketPrice ;
			}			
		}

		// 売り注文よりも高い買い注文があった場合成立
		//注文のタイプが売り　かつ　買いの価格が売りの価格よりも大きいなら
		else if(tradeFlag == Flag.Trade.SELL && sellPrice <= buyPrice) {

			//注文価格を取引注文価格に
			this.execBuyOrderPrice=buyPrice;
			this.execSellOrderPrice=sellPrice;

			//買いのエージェントがHFTなら
			if(buy.getOrderAgent() instanceof HFT){
				//HFTの注文が買いの成り行き注文なら
				if(this.nariyuki==true){
					//買い指値
					this.buyOrderAgent = NOTHING;
					//売り指値
					this.sellOrderAgent = NORMAL;
					//買い成行
					this.buyNewOrderAgent = HFTAGENT;
					//売り成行
					this.sellNewOrderAgent = NOTHING;
					//指値が「売り」の場合
					// 直近の市場価格を指値注文の価格に
					this.marketPrice = sellPrice;
					// 約定時の処理を行い、成行が左、指値が右
					vol += execOrder(buy, sellBook);
				}else{
					//買い指値
					this.buyOrderAgent = HFTAGENT;
					//売り指値
					this.sellOrderAgent = NOTHING;
					//買い成行
					this.buyNewOrderAgent = NOTHING;
					//売り成行
					this.sellNewOrderAgent = NORMAL;
					//指値が「買い」の場合
					// 直近の市場価格を一番高い買いの価格に
					this.marketPrice = buyPrice;
					// 約定時の処理を行い，出来高をvolに加算
					vol += execOrder(sell, buyBook);
				}
				//HFTポジション更新
				this.hftPosition+=1;
			}
			//買いのエージェントがNAなら
			else if(buy.getOrderAgent() instanceof NormalAgent || buy.getOrderAgent() instanceof FundamentalAgent || buy.getOrderAgent() instanceof TechnicalAgent){
				//売りのエージェントがHFTなら
				if(sell.getOrderAgent() instanceof HFT){
					//買い指値
					this.buyOrderAgent = NORMAL;
					//売り指値
					this.sellOrderAgent = NOTHING;
					//買い成行
					this.buyNewOrderAgent = NOTHING;
					//売り成行
					this.sellNewOrderAgent = HFTAGENT;
					//HFTポジション更新
					// 21/5/30 +=-1 -> -= 1
					this.hftPosition -= 1;
				}
				//売りのエージェントがNAなら
				else if(sell.getOrderAgent() instanceof NormalAgent || sell.getOrderAgent() instanceof FundamentalAgent || sell.getOrderAgent() instanceof TechnicalAgent){
					//買い指値
					this.buyOrderAgent = NORMAL;
					//売り指値
					this.sellOrderAgent = NOTHING;
					//買い成行
					this.buyNewOrderAgent = NOTHING;
					//売り成行
					this.sellNewOrderAgent = NORMAL;
				}
				//指値が「買い」の場合
				// 直近の市場価格を一番高い買いの価格に
				this.marketPrice = buyPrice;
				// 約定時の処理を行い，出来高をvolに加算
				vol += execOrder(sell, buyBook);
			}
			// １日あたりの出来高にvolを加算
			this.dayVolume += vol;
			// 出来高にvolを加算
			this.volume += vol;
			
			// エージェントタイプごとの取引額を集計
			// 買い注文の集計
			if (buy.getOrderAgent() instanceof FundamentalAgent) {
				fundamentalAgentTotalBuy += this.marketPrice * vol;
				fundamentalAgentBuyCount++;
			} else if (buy.getOrderAgent() instanceof TechnicalAgent) {
				technicalAgentTotalBuy += this.marketPrice * vol;
				technicalAgentBuyCount++;
			}
			
			// 売り注文の集計
			if (sell.getOrderAgent() instanceof FundamentalAgent) {
				fundamentalAgentTotalSell += this.marketPrice * vol;
				fundamentalAgentSellCount++;
			} else if (sell.getOrderAgent() instanceof TechnicalAgent) {
				technicalAgentTotalSell += this.marketPrice * vol;
				technicalAgentSellCount++;
			}

			// 21/6/2 HFTのNAV計算
			if (this.buyOrderAgent == HFTAGENT || this.buyNewOrderAgent == HFTAGENT) {
				this.hft_NAV += this.marketPrice ;
			}
			if (this.sellOrderAgent == HFTAGENT || this.sellNewOrderAgent == HFTAGENT) {
				this.hft_NAV -= this.marketPrice ;
			}			
		}

		else{
			this.execBuyOrderPrice=0;
			this.execSellOrderPrice=0;
			this.buyOrderAgent = NOTHING;
			this.sellOrderAgent = NOTHING;
			this.buyNewOrderAgent = NOTHING;
			this.sellNewOrderAgent = NOTHING;
		}
// 以上

		// 注文数量が0となった注文を板から削除
		removeZeroOrder(buyBook, sellBook);

		// 市場価格の最大値と最小値の更新
		if(this.marketPrice > this.marketPriceMax) {
			this.marketPriceMax = this.marketPrice;
		}
		else if(this.marketPrice < this.marketPriceMin) {
			this.marketPriceMin = this.marketPrice;
		}
	}

	// 約定時の処理を行い，出来高を返す
	public double execOrder(Order order, List<Order> orderBook) {
		// 出来高

		//volumeの初期化
		double volume = 0;
		//注文数量を設定
		int orderNum = order.getOrderNum();
		//注文リストが終わるまでループ
		for(int i=0; i<orderBook.size(); i++) {
			//注文リストのi番目の注文情報を取得
			Order oneOrder = orderBook.get(i);
			//注文数量を取得
			int oneOrderNum = oneOrder.getOrderNum();
			// 注文数量が同じ場合
			if(orderNum - oneOrderNum == 0) {
				// 出来高に注文数量を追加
				volume += orderNum;
				// 約定した注文によるポジションの変更
				setOrderPosition(order);
				setOrderPosition(oneOrder);
				// 約定した注文の削除
				order.removeOrderNum(orderNum);
				oneOrder.removeOrderNum(oneOrderNum);
				break;
			}

			// 注文数量が多い場合
			else if(orderNum - oneOrderNum > 0) {
				// 出来高に一回分の注文数量を追加
				volume += oneOrderNum;
				// 残りの注文数量を更新
				orderNum -= oneOrderNum;
				// 約定した注文によるポジションの変更
				setOrderPosition(oneOrder);
				// 約定した注文の削除
				oneOrder.removeOrderNum(oneOrderNum);
			}

			// 注文数量が少ない場合
			else {
				volume += orderNum;
				// 約定した注文によるポジションの変更
				setOrderPosition(order);
				setOrderPosition(oneOrder);
				// 約定した注文の削除
				oneOrder.removeOrderNum(orderNum);
				order.removeOrderNum(orderNum);
				break;
			}
		}
		return volume;
	}

	// 約定した注文によるポジションの変更
	private void setOrderPosition(Order order) {
		Agent agent = order.getOrderAgent();
		agent.updatePosition(order.getTradeFlag(), order.getOrderNum());
	}

	// 市場価格の時系列データを更新
	public void updateMarketPrice(int time) {
		this.marketPriceArray[time] = marketPrice;
	}

	// 前期のHFTエージェントの注文を削除
	public void removeHFTOldOrder() {
		buyBook.removeIf(x -> x.getOrderAgent() instanceof HFT);
		sellBook.removeIf(x -> x.getOrderAgent() instanceof HFT);
	}

	// 板情報の表示
	public void dispBook() {
		int bookSize = 15;
		List<Order> buyBookHead = this.buyBook.subList(0, bookSize+1);
		List<Order> sellBookHead = this.sellBook.subList(0, bookSize+1);
		System.out.println("====================================");
		System.out.print("sell");
		dispBookNum(sellBookHead);
		dispBookNum(buyBookHead);
		System.out.print("buy\n");
		System.out.println("====================================");
	}
	private void dispBookNum(List<Order> book) {
		double orderPrice = 0.0;
		for(Order order: book) {
			int orderNum = order.getOrderNum();
			if(orderPrice != order.getOrderPrice()) {
				orderPrice = order.getOrderPrice();
				System.out.printf("\n%.1f: ", orderPrice);
			}
			for(int i=0; i< orderNum; i++) {
				System.out.print("*");
			}
		}
		System.out.print("\n");
	}
	public double[] getAllBook(){
		List<Order> buyAndSell = new ArrayList<Order>();
		buyAndSell.addAll(this.buyBook);
		buyAndSell.addAll(this.sellBook);
		return buyAndSell.stream().mapToDouble(x -> x.getOrderPrice()).toArray();
	}

	// 注文数量が0の注文をリストから削除
	private void removeZeroOrder(List<Order> buyBook, List<Order> sellBook) {
		buyBook.removeIf(buy -> buy.getOrderNum() == 0);
		sellBook.removeIf(sell -> sell.getOrderNum() == 0);
	}

	// １日ごとのに市場価格の最大値と最小値をリセット
	public void resetMarket_MaxMin() {
		this.marketPriceMax = this.marketPrice;
		this.marketPriceMin = this.marketPrice;
	}

	// 時刻t分過去の市場価格を取得
	public double getPreMarketPrice(int t, int time) {
		// 市場価格がシミュレーション開始からどれだけ経過しているかの数
		int lengthMarket = time - 1;
		// 過去の市場価格
		double tMarketPrice;

		// t分過去のデータが存在する場合
		if(lengthMarket - t > 1 && t > 0) {
			tMarketPrice = this.marketPriceArray[lengthMarket - t];
		}
		// 存在しない場合初期値
		else {
			tMarketPrice = this.marketPriceArray[0];
		}
		return tMarketPrice;
	}

	// 出来高のリセット
	public void resetDayVolume() {
		this.dayVolume = 0;
	}

	// 板に注文があるかどうかの確認
	public boolean bookEmpty() {
		if(this.buyBook.isEmpty() || this.sellBook.isEmpty()) {
			return true;
		}
		else {
			return false;
		}
	}

	// getter
	public double getMarketPrice() {
		return this.marketPrice;
	}
	public double getMarketPriceMax() {
		return this.marketPriceMax;
	}
	public double getMarketPriceMin() {
		return this.marketPriceMin;
	}
	public double getVolume() {
		return this.volume;
	}
	public double getDayVolume() {
		return this.dayVolume;
	}

	// 最良買い価格
	public double getBestBid() {
		if(!(this.buyBook.isEmpty())) {
			return this.buyBook.get(0).getOrderPrice();
		}
		else {
			return 0;
		}
	}
	
	// 21/5/16 NAの最良買い気配値
	public double getBestBidNA() {
		// 買い板のサイズが1だとgetBuy()でNAの最良買い気配値は得られているので0を返す．
		if(this.buyBook.size() > 1) {
			// 買い板のサイズが2以上で最良買い気配がHFTの注文だとその次の注文の価格を返す．
			if (this.buyBook.get(0).getOrderAgent() instanceof HFT){
				return this.buyBook.get(1).getOrderPrice();
			}
			else return 0.0;
		}
		else {
			return 0.0;
		}
	}
	
    // 25/4/5 次点の最良買い気配値
	public double getSecondBestBid() {
		if(this.buyBook.size() > 1) {
			// 買い板のサイズが2以上で次点の最良買い気配値を返す
			// HFTの注文が存在する場合は考慮していない
			return this.buyBook.get(1).getOrderPrice();
		}
		else {
			return 0.0;
		}
	}	

	// 最良売り価格
	public double getBestAsk() {
		if(!(this.sellBook.isEmpty())) {
			return this.sellBook.get(0).getOrderPrice();
		}
		else {
			return 0;
		}
	}

	// 21/5/16 NAの最良売り気配値
	public double getBestAskNA() {
		// 売り板のサイズが1だとgetSell()でNAの最良売り気配値は得られているので0を返す．
		if(this.sellBook.size() > 1) {
			// 売り板のサイズが2以上で最良売り気配がHFTの注文だとその次の注文の価格を返す．
			if (this.sellBook.get(0).getOrderAgent() instanceof HFT){
				return this.sellBook.get(1).getOrderPrice();
			}
			else return 0.0;
		}
		else {
			return 0.0;
		}
	}

	// 25/04/05 次点の最良売り気配値
	public double getSecondBestAsk() {
		if(this.sellBook.size() > 1) {
			// 売り板のサイズが2以上で次点の最良売り気配値を返す
			// HFTの注文が存在する場合は考慮していない
			return this.sellBook.get(1).getOrderPrice();
		}
		else {
			return 0;
		}
	}

	// 買いのデプス
	public int getBuyDepth() {
		return (int)(this.buyBook.stream().filter(buy -> buy.getOrderPrice() >= (this.getBestBid() - tickDepth)).count());
	}

	// 売りのデプス
	public int getSellDepth() {
//以下修正：売り板ではなく買い板から参照していたので修正
		return (int)(this.sellBook.stream().filter(sell -> sell.getOrderPrice() <= (this.getBestAsk() + tickDepth)).count());
//以上
	}
	
	// 21/5/16
//	public double getOrderImbalance() {
//		return (((double)this.getAmountBuyBook() - this.getAmountSellBook())/(this.getAmountBuyBook() + this.getAmountSellBook()));
//	}


//以下追加実装：出力用データを返す処理
	//買いの取引価格を返す
	public double getExecBuyOrderPrice() {
		return this.execBuyOrderPrice;
	}
	//売りの取引価格を返す
	public double getExecSellOrderPrice() {
		return this.execSellOrderPrice;
	}
	//取引が成立した、買いの指値注文の注文主エージェント情報を返す
	public String getExecBuyOrderAgent() {
		return this.buyOrderAgent;
	}
	//取引が成立した、売りの指値注文の注文主エージェント情報を返す
	public String getExecSellOrderAgent() {
		return this.sellOrderAgent;
	}
	//買い板のすべての注文の数を返す
	public int getAmountBuyBook() {
		return (int)this.buyBook.size();
		// 21/5/8 変更
		//return (int)(this.buyBook.stream().filter(buy -> buy.getOrderPrice() >= 1.0).count());

	}
	//売り板のすべての注文の数を返す
	public int getAmountSellBook() {
		return (int)this.sellBook.size();
	}
	
	//HFTの買いの注文価格を返す 22/2/9 名称変更 
	public double getAdjustHFTBuyOrderPrice() {
		return this.hftBuyOrderPrice;
	}
	//HFTの売りの注文価格を返す 22/2/9 名称変更
	public double getAdjustHFTSellOrderPrice() {
		return this.hftSellOrderPrice;
	}

	// 最良買い気配値の注文主エージェント情報を返す
	public String getBestBidAgent() {
		if(!(this.buyBook.isEmpty())) {
			//最良買い価格の注文主がHFTなら
			if(this.buyBook.get(0).getOrderAgent() instanceof HFT){
				this.bestBidAgent = HFTAGENT;
			}
			//最良買い価格の注文主がNAなら
			else if(this.buyBook.get(0).getOrderAgent() instanceof NormalAgent){
				this.bestBidAgent = NORMAL;
			}
			//最良買い価格の注文主がFundamentalAgentなら（追加）
			else if(this.buyBook.get(0).getOrderAgent() instanceof FundamentalAgent){
				this.bestBidAgent = NORMAL; // ここでは同じく"N"として扱う
			}
			//最良買い価格の注文主がTechnicalAgentなら（追加）
			else if(this.buyBook.get(0).getOrderAgent() instanceof TechnicalAgent){
				this.bestBidAgent = NORMAL; // ここでは同じく"N"として扱う
			}
		}
		else {
			this.bestBidAgent = NOTHING;
		}
		return this.bestBidAgent;
	}

	// 最良売り気配値の注文主エージェント情報を返す
	public String getBestAskAgent() {
		if(!(this.sellBook.isEmpty())) {
			//最良売り価格の注文主がHFTなら
			if(this.sellBook.get(0).getOrderAgent() instanceof HFT){
				this.bestAskAgent = HFTAGENT;
			}
			//最良売り価格の注文主がNAなら
			else if(this.sellBook.get(0).getOrderAgent() instanceof NormalAgent){
				this.bestAskAgent = NORMAL;
			}
			//最良売り価格の注文主がFundamentalAgentなら（修正）
			else if(this.sellBook.get(0).getOrderAgent() instanceof FundamentalAgent){
				this.bestAskAgent = NORMAL; // ここでは同じく"N"として扱う
			}
			//最良売り価格の注文主がTechnicalAgentなら（修正）
			else if(this.sellBook.get(0).getOrderAgent() instanceof TechnicalAgent){
				this.bestAskAgent = NORMAL; // ここでは同じく"N"として扱う
			}
		}
		else {
			this.bestAskAgent = NOTHING;
		}
		return this.bestAskAgent;
	}

	//HFTのポジションを返す
	public int getHftPosition() {
		return this.hftPosition;
	}

	//取引が成立した、買いの成行注文の注文主エージェント情報を返す
	public String getNewExecBuyOrderAgent() {
		return this.buyNewOrderAgent;
	}

	//取引が成立した、売りの成行注文の注文主エージェント情報を返す
	public String getNewExecSellOrderAgent() {
		return this.sellNewOrderAgent;
	}

	//毎期ごとの期数を返す
//	public int getTurn() {
//		this.turnCount++;
//		return this.turnCount;
//	}
//	
	// 21/5/30
	public int getHftNetAssetValue() {
		return this.hft_NAV ;
	}
//以上


	public double[] getMarketPriceArray() {
		return marketPriceArray;
	}

	// 2024/07/17
	// instantVolumeを更新
	public double updateinstantVolume() {
		this.instantVolume += 1;
		return instantVolume;
	}
	// limitVolumeを更新
	public double updatelimitVolume() {
		this.limitVolume += 1;
		return limitVolume;
	}
	
	// 2024/07/17
	// instantVolumeを確認
	public double getinstantVolume() {
		return this.instantVolume;
	}
	// limitVolumeを確認
	public double getlimitVolume() {
		return this.limitVolume;
	}
	
	// 2024/07/24
	// InstantToLimitVolumeを更新
	public double updateInstantToLimitVolume() {
		this.InstantToLimitVolume += 1;
		return InstantToLimitVolume;
	}
	// LimitToInstantVolumeを更新
	public double updateLimitToInstantVolume() {
		this.LimitToInstantVolume += 1;
		return LimitToInstantVolume;
	}
	
	// 2024/07/24
		// InstantToLimitVolumeを更新
		public double getInstantToLimitVolume() {
			return InstantToLimitVolume;
		}
		// LimitToInstantVolumeを更新
		public double getLimitToInstantVolume() {
			return LimitToInstantVolume;
		}
		
		// エージェントタイプごとの収益を取得するメソッド
		public double getFundamentalAgentProfit() {
			return fundamentalAgentTotalSell - fundamentalAgentTotalBuy;
		}
		
		public double getFundamentalAgentTotalBuy() {
			return fundamentalAgentTotalBuy;
		}
		
		public double getFundamentalAgentTotalSell() {
			return fundamentalAgentTotalSell;
		}
		
		public int getFundamentalAgentBuyCount() {
			return fundamentalAgentBuyCount;
		}
		
		public int getFundamentalAgentSellCount() {
			return fundamentalAgentSellCount;
		}
		
		public double getTechnicalAgentProfit() {
			return technicalAgentTotalSell - technicalAgentTotalBuy;
		}
		
		public double getTechnicalAgentTotalBuy() {
			return technicalAgentTotalBuy;
		}
		
		public double getTechnicalAgentTotalSell() {
			return technicalAgentTotalSell;
		}

		public double getTechnicalAgentBuyCount() {
			return technicalAgentBuyCount;
		}

		public double getTechnicalAgentSellCount() {
			return technicalAgentSellCount;
		}
}
