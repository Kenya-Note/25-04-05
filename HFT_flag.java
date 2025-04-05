// HFTエージェント種別を列挙型にて宣言
public enum HFT_flag {
	NONE,	// HFTなし
	SMM,	// シンプルマーケットメイカー
	nPMM,	// ポジションマーケットメイカー（成行注文なし）
	mPMM,	// ポジションマーケットメイカー（成行注文あり）
	OMM,	// オーダーインバランスマーケットメイカー
	rOMM,	// オーダーインバランス（逆張り注文）マーケットメイカー
	POMM,	// nPMM & OMM
	PrOMM	// nPMM & rOMM
}