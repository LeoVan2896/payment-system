import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import axiosClient from "../components/axiosClient";
import { useNavigate } from "react-router-dom";
import { AccountCard, TransferForm, DepositForm } from "../components";

function DashboardPage() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [accounts, setAccounts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!user) return navigate("/login");
        const fetchAccounts = async () => {
            try {
                const response = await axiosClient.get(`/api/accounts/user/${user.id}`);
                setAccounts(response.data);
                setLoading(false);
            } catch {
                setError("Failed to load accounts. Please refresh.");
                setLoading(false);
            }
        };
        fetchAccounts();
    }, [user, navigate]);

    const handleCreateAccount = async () => {
        try {
            const response = await axiosClient.post(`/api/accounts/user/${user.id}`);
            setAccounts(prev => [...prev, response.data]);
        } catch {
            setError("Failed to create account.");
        }
    };

    if (loading) return <div className="loading-state">Loading your accounts...</div>;
    if (error) return <div className="error-state">{error}</div>;

    return (
        <div>
            <nav className="navbar">
                <div className="navbar-brand">
                    <div className="navbar-logo">💳</div>
                    PayFlow
                </div>
                <div className="navbar-user">
                    <span className="navbar-greeting">Welcome back, {user.firstName}</span>
                    <button
                        className="btn btn-outline btn-sm"
                        onClick={() => { logout(); navigate("/login"); }}
                    >
                        Sign Out
                    </button>
                </div>
            </nav>

            <main className="dashboard-main">
                <div className="section-header">
                    <h2>My Accounts</h2>
                    <button className="btn btn-primary" onClick={handleCreateAccount}>
                        + New Account
                    </button>
                </div>

                <div className="accounts-grid">
                    {accounts.length === 0 ? (
                        <div className="empty-state">
                            <div className="empty-state-icon">🏦</div>
                            <p>No accounts yet. Create your first account to get started!</p>
                        </div>
                    ) : (
                        accounts.map(account => (
                            <AccountCard key={account.id} account={account} />
                        ))
                    )}
                </div>

                <h2 className="forms-title">Transactions</h2>
                <div className="forms-section">
                    <TransferForm accounts={accounts} />
                    <DepositForm accounts={accounts} />
                </div>
            </main>
        </div>
    );
}

export default DashboardPage;
