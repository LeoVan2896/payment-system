function AccountCard({ account }) {
    const isActive = account.status?.toLowerCase() === "active";

    return (
        <div className="account-card">
            <div className="account-card-label">Account Balance</div>
            <div className="account-card-balance">
                <span className="currency">$</span>
                {Number(account.balance).toFixed(2)}
            </div>
            <div className="account-card-footer">
                <span className={`status-badge ${isActive ? "active" : "inactive"}`}>
                    {isActive ? "● Active" : "○ Inactive"}
                </span>
                <span className="account-card-number">{account.accountNumber}</span>
            </div>
        </div>
    );
}

export default AccountCard;
