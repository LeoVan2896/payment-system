import { useState } from "react";
import axiosClient from "./axiosClient";

function DepositForm({ accounts }) {
    const [accountNumber, setAccountNumber] = useState("");
    const [amount, setAmount] = useState("");
    const [message, setMessage] = useState({ text: "", type: "" });

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!accountNumber || !amount) {
            setMessage({ text: "Please fill in all fields.", type: "error" });
            return;
        }
        try {
            await axiosClient.post(`/api/accounts/${accountNumber}/deposit`, {
                amount: String(parseFloat(amount)),
            });
            setMessage({ text: "Deposit successful!", type: "success" });
            setAmount("");
            setAccountNumber("");
        } catch (error) {
            setMessage({
                text: error.response?.data?.message || "Deposit failed.",
                type: "error",
            });
        }
    };

    return (
        <div className="form-card">
            <h3 className="form-card-title">↓ Deposit Funds</h3>
            <form onSubmit={handleSubmit} className="form-card-body">
                <div className="form-group">
                    <label className="form-label">Account</label>
                    <select
                        className="form-select"
                        value={accountNumber}
                        onChange={(e) => setAccountNumber(e.target.value)}
                    >
                        <option value="">— Select account —</option>
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
                    Deposit
                </button>
            </form>
        </div>
    );
}

export default DepositForm;
