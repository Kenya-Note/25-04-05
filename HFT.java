public class HFT extends Agent{

	// 所持しているポジション
	// 買いポジション：+1
	// 売りポジション：-1
	private int position;
	// 買い注文価格
	private double buyOrderPrice;
	// 調整後買い注文価格
	private double adjustBuyOrderPrice;
	// 売り注文価格
	private double sellOrderPrice;
	// 調整後売り注文価格
	private double adjustSellOrderPrice;
	// エージェントの注文数
	private static int ORDER_NUM = 1;
	
	//21/5/13
	private double fairValue ;
	
	private int performance ;
	
	public HFT() {
		// ポジション 21/5/13 this.
		this.position = 0;
		
		this.fairValue = 0.0;
		
		// 21/6/5
		this.performance = 0 ;
	}

	// 注文の発注
	// 21/4/26 HFT_flagパラメータを追加
	public void order(Market market, int time, Flag.HFT hft_flag, Parameter param) {
		// 前期のHFTエージェントの注文の取消
		market.removeHFTOldOrder();
		// 注文価格の計算
		// 21/4/26 HFT_flagパラメータを追加
		// 21/12/16 期を追加
		calcOrderPrice(market, time, hft_flag, param);
		// 買い注文
		//修正：列挙型での宣言に伴い、引数を変更
		Order orderBuy = new Order(this, time, adjustBuyOrderPrice, ORDER_NUM, Flag.Trade.BUY);
		market.addOrder(orderBuy);
		// 売り注文
		//修正：列挙型での宣言に伴い、引数を変更
		Order orderSell = new Order(this, time, adjustSellOrderPrice, ORDER_NUM, Flag.Trade.SELL);
		market.addOrder(orderSell);
	}

	// フェアバリュの計算
	// 21/4/26 HFT_flagパラメータを追加
	// 21/5/13 fairValue -> fv CSV出力対応
	// 21/12/16 期の追加
	private double calcFairValue(Market market, int time, double bestAsk, double bestBid, Flag.HFT hft_flag, Parameter param) {
		
		//最良売買気配の仲値
		double mid_price  = (bestBid + bestAsk) / 2;

		double fv = 0.0 ;

		// 21/4/27 オーダーインバランス（買い注文数-売り注文数）
		double orderImbalance_weight = 0.0 ;
		// 21/6/4 ポジション
		double position_weight = 0.0 ;
		
//		//PMM戦略の計算式
//		double fairValue = (1 - (param.getW_pm() * Math.pow(position, 3))) * avgBidAsk;

//以下追加実装：フェアバリュの計算
		// 変更21/4/26
		if (hft_flag == Flag.HFT.SMM) {
			fv = mid_price ;
		} else if ((hft_flag == Flag.HFT.nPMM) ||(hft_flag == Flag.HFT.mPMM)) {
			
			// 21/6/4 
			//fv = (1 - (param.getW_pm() * Math.pow(position, 3))) * mid_price ;
			position_weight = this.getPositionWeight(param);
			fv = (1 - position_weight) * mid_price ;
		} else if (hft_flag == Flag.HFT.OMM) {
			
			// 21/6/4 market -> this
			// 21/12/16 期timeの追加
			orderImbalance_weight = this.getOrderImbalanceWeight(market, time, param) ;
			fv = (1 + orderImbalance_weight) * mid_price ; 
		} else if (hft_flag == Flag.HFT.rOMM) {
			
			// 21/6/20
			// 21/12/16 期timeの追加
			orderImbalance_weight = this.getOrderImbalanceWeight(market, time, param) ;
			fv = (1 - orderImbalance_weight) * mid_price ; 
		} else if (hft_flag == Flag.HFT.POMM) {

			// 21/6/4
			// 21/12/16 期timeの追加
			position_weight = this.getPositionWeight(param);
			orderImbalance_weight = this.getOrderImbalanceWeight(market, time, param) ;
			fv = (1 - position_weight + orderImbalance_weight) * mid_price ;
			//fv = (1 - position_weight) * (1 + orderImbalance_weight) * mid_price ;
		} else if (hft_flag == Flag.HFT.PrOMM) {

			// 21/6/24
			// 21/12/16 期timeの追加
			position_weight = this.getPositionWeight(param);
			orderImbalance_weight = this.getOrderImbalanceWeight(market, time, param) ;
			fv = (1 - position_weight - orderImbalance_weight) * mid_price ;
			//fv = (1 - position_weight) * (1 - orderImbalance_weight) * mid_price ;
	}
//以上

		return fv;
	}

	// 21/4/26 HFT_flagパラメータを追加
	// 21/5/13 fv -> fairValue CSV出力対応
	// 21/12/16 期timeの追加
	private void calcOrderPrice(Market market, int time, Flag.HFT hft_flag, Parameter param) {
		// 最良買い気配
		double bestBid = market.getBestBid();
		// 最良売り気配
		double bestAsk = market.getBestAsk();
		// フェアバリュ
		// 21/4/26 HFT_flagパラメータを追加
		// 21/12/16 期timeの追加		
		fairValue = calcFairValue(market, time, bestBid, bestAsk, hft_flag, param);
		// 注文価格
			buyOrderPrice = fairValue - (param.getP_fund() * param.getTheta() / 2);
			sellOrderPrice = fairValue + (param.getP_fund() * param.getTheta() / 2);

		// 買い注文価格が板の最良売り気配を上回ると成行注文になってしまうので補正
		// 21/4/26 mPMM以外の価格調整
		if (hft_flag != Flag.HFT.mPMM) {
			if(buyOrderPrice >= bestAsk){
				buyOrderPrice = bestAsk - param.getDelta_p();
				// 21/5/13 バグ修正 fairValue -> P_fund
				sellOrderPrice = buyOrderPrice + (param.getP_fund() * param.getTheta());
			}else if(sellOrderPrice <= bestBid) {
				sellOrderPrice = bestBid + param.getDelta_p();
				// 21/5/13 バグ修正 fairValue -> P_fund
				buyOrderPrice = sellOrderPrice - (param.getP_fund() * param.getTheta());
			}
		// 21/5/13 mPMMのときの価格調整（成行注文価格が想定外な価格にならないようを1tickずらすだけにする．）
		// ただし発注数が1以上だと，ベストビッド（アスク）の注文数以上の成行があると，
		// 成行ではなく指値になってしまう可能性があるので再度検討要．
		} else {
			if (buyOrderPrice >= bestAsk) {
				buyOrderPrice = bestAsk + param.getDelta_p() ;
				sellOrderPrice = buyOrderPrice + (param.getP_fund() * param.getTheta()) ;
			} else if (sellOrderPrice <= bestBid) {
				sellOrderPrice = bestBid - param.getDelta_p();
				// 21/5/13 バグ修正 fairValue -> P_fund
				buyOrderPrice = sellOrderPrice - (param.getP_fund() * param.getTheta());
			}
		}

		// ティックサイズによる調整
		adjustBuyOrderPrice = (Math.floor(buyOrderPrice / (double)param.getDelta_p()) * param.getDelta_p());
		adjustSellOrderPrice = (Math.ceil(sellOrderPrice / (double)param.getDelta_p()) * param.getDelta_p());
	}


	// ポジションの更新
	@Override
	public void updatePosition(Flag.Trade tradeFlag, int orderVol) {
		// 買い注文：+1
		if(tradeFlag == Flag.Trade.BUY) {
			this.position = this.position + orderVol;
		}
		// 売り注文：-1
		else if(tradeFlag == Flag.Trade.SELL) {
			this.position = this.position - orderVol;
		}
	}
	
	// 21/5/13
	public double getFairValue() {
		return this.fairValue;
	}
	
	// 21/6/4 flg=true 買い板 > 売り板 -> 正値（基準価格が上がるので買い注文が成立しやすくなる）
	//		  flg=false 買い板 > 売り板 -> 負値（基準価格が下がるので売り注文が成立しやすくなる）
	public double getOrderImbalanceWeight(Market market, int time, Parameter param) {
		
		int numberofBuyBook = 0 ;
		int numberofSellBook = 0 ;
		
		// 21/12/8
		int numberofBuyDepth = 0 ;
		int numberofSellDepth = 0 ;
		
		double orderImbalanceWeight = 0.0 ;
		
		// 21/9/30
		double diff_Depth = 0.0 ;
		
		// 21/12/16 見せ玉注文フラグ（買い：true, 売り：false）
		//boolean spoofer_order_flag = true ;
		boolean spoofer_flag = param.getSpoofer_flag() ;
		int spoofer_strt = param.getSpoofer_strt() ;
		int spoofer_interval = param.getSpoofer_interval();
		
		numberofBuyBook = market.getAmountBuyBook();
		numberofSellBook = market.getAmountSellBook();
		
		// 21/12/8
		numberofBuyDepth = market.getBuyDepth();
		numberofSellDepth = market.getSellDepth();
		
		// 21/12/16
		if (spoofer_flag) {
			if (time > spoofer_strt) {
				if (((time / spoofer_interval) % 2) == 0) {
					numberofBuyDepth += param.getNumberofspoofer();
					numberofBuyBook += param.getNumberofspoofer();
				} else {
					numberofSellDepth += param.getNumberofspoofer();
					numberofSellBook += param.getNumberofspoofer();
				}
			}
		}
		// 21/12/8
		// diff_orders = (double)numberofBuyBook - numberofSellBook ;
		// orderImbalanceWeight = param.getW_om() * Math.signum(diff_orders) * Math.log(Math.abs(diff_orders)) ;
		// 21/12/12 3乗から1乗へ．
//		orderImbalanceWeight = param.getW_om() * Math.pow((((double)numberofBuyDepth - numberofSellDepth)/(numberofBuyBook + numberofSellBook)), 3) ;
		diff_Depth = (double)numberofBuyDepth - numberofSellDepth ;
		orderImbalanceWeight = param.getW_om() * Math.signum(diff_Depth)* Math.pow((Math.abs(diff_Depth) /(numberofBuyBook + numberofSellBook)), 2) ;
		
		return orderImbalanceWeight ;
	}

	// 21/6/4
	public double getPositionWeight(Parameter param) {
		
		double positionWeight = 0.0 ;
				
		positionWeight = param.getW_pm() * Math.pow(this.position, 3) ;

		return positionWeight ;
	}
	
	public double getHFTperformance(Market market) {
		return market.getMarketPrice() * this.position -(double)market.getHftNetAssetValue() ;
	}
}
