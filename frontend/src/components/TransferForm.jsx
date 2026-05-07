import { useState } from "react";
import axiosClient from "./axiosClient";

function TransferForm({ accounts }) {
    const [senderAccountNumber, setSenderAccountNumber] = useState("");
    const [receiverAccountNumber, setReceiverAccountNumber] = useState("");
    const [amount, setAmount] = useState("");
    const [message, setMessage] = useState({ text: "", type: "" });

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!senderAccountNumber || !receiverAccountNumber || !amount) {
            setMessage({ text: "Please fill in all fields.", type: "error" });
            return;
        }
        try {
            await axiosClient.post("/api/accounts/transfer", {
                senderAccountNumber,
                receiverAccountNumber,
                amount: String(parseFloat(amount)),
            });
            setMessage({ text: "Transfer successful!", type: "success" });
            setAmount("");
            setSenderAccountNumber("");
            setReceiverAccountNumber("");
        } catch (error) {
            setMessage({
                text: error.response?.data?.message || "Transfer failed.",
                type: "error",
            });
        }
    };

    return (
        <div className="form-card">
            <h3 className="form-card-title">⇄ Transfer Funds</h3>
            <form onSubmit={handleSubmit} className="form-card-body">
                <div className="form-group">
                    <label className="form-label">From Account</label>
                    <select
                        className="form-select"
                        value={senderAccountNumber}
                        onChange={(e) => setSenderAccountNumber(e.target.value)}
                    >
                        <option value="">— Select source account —</option>
                        {accounts.map(acc => (
                            <option key={acc.id} value={acc.accountNumber}>
                                {acc.accountNumber} · ${Number(acc.balance).toFixed(2)}
                            </option>
                        ))}
                    </select>
                </div>
                <div className="form-group">
                    <label className="form-label">To Account</label>
                    <select
                        className="form-select"
                        value={receiverAccountNumber}
                        onChange={(e) => setReceiverAccountNumber(e.target.value)}
                    >
                        <option value="">— Select destination account —</option>
                        {accounts.map(acc => (
                            <option key={acc.id} value={acc.accountNumber}>
                                {acc.accountNumber} · ${Number(acc.balance).toFixed(2)}
                            </option>
                        ))}
                    </select>
                </div>
                <div className="form-group">
                    <label className="form-label">Amount</label>
                    <input
                        className="form-input"
                        type="number"
                        placeholder="0.00"
                        min="0.01"
                        step="0.01"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                    />
                </div>

                {message.text && (
                    <div className={message.type === "success" ? "form-success" : "form-error"}>
                        {message.text}
                    </div>
                )}

                <button type="submit" className="btn btn-primary btn-full">
                    Transfer
                </button>
            </form>
        </div>
    );
}

export default TransferForm;
