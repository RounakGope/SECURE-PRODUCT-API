import { useEffect, useMemo, useState } from "react";
import {
  Activity, ArrowDownToLine, Box, Check, ChevronRight, CircleUserRound,
  Database, Eye, EyeOff, KeyRound, LogOut, Mail, Package, Pencil,
  Plus, RefreshCw, Search, ShieldCheck, Trash2, X
} from "lucide-react";
import api, { getErrorMessage } from "./api";

const emptyProduct = { name: "", description: "", price: "", stock: "", category: "" };

function App() {
  const [token, setToken] = useState(() => localStorage.getItem("psa_token") || "");
  const [authMode, setAuthMode] = useState("login");
  const [authForm, setAuthForm] = useState({ name: "", email: "", password: "" });
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState("");
  const [userEmail, setUserEmail] = useState(() => localStorage.getItem("psa_email") || "");
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pageError, setPageError] = useState("");
  const [notice, setNotice] = useState("");
  const [search, setSearch] = useState("");
  const [modal, setModal] = useState(null);
  const [productForm, setProductForm] = useState(emptyProduct);
  const [saving, setSaving] = useState(false);
  const [otp, setOtp] = useState("");
  const [otpBusy, setOtpBusy] = useState(false);
  const [showOtp, setShowOtp] = useState(false);
  const [emailVerified, setEmailVerified] = useState(false);

  const visibleProducts = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return products;
    return products.filter((p) =>
      [p.name, p.description, p.category, String(p.id)].some((v) =>
        String(v ?? "").toLowerCase().includes(term)
      )
    );
  }, [products, search]);

  useEffect(() => {
    if (token) loadProducts();
    // Token change is the intended trigger.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  async function loadProducts() {
    setLoading(true);
    setPageError("");
    try {
      const response = await api.get("/api/products");
      const body = response.data;
      setProducts(Array.isArray(body) ? body : Array.isArray(body?.content) ? body.content : []);
    } catch (error) {
      setPageError(getErrorMessage(error));
      if (error?.response?.status === 401) logout(false);
    } finally {
      setLoading(false);
    }
  }

  async function submitAuth(event) {
    event.preventDefault();
    setAuthBusy(true);
    setAuthError("");
    try {
      if (authMode === "register") {
        await api.post("/api/auth/register", {
          name: authForm.name.trim(),
          email: authForm.email.trim(),
          password: authForm.password
        });
        setNotice("Account created. Log in with your credentials, then verify your email from the dashboard if desired.");
        setAuthMode("login");
      } else {
        const response = await api.post("/api/auth/login", {
          email: authForm.email.trim(),
          password: authForm.password
        });
        // AuthResponse in the backend was described as (token, "Bearer").
        const data = response.data || {};
        const receivedToken = data.token ?? data.accessToken ?? data.jwt;
        if (!receivedToken) {
          throw new Error("Login succeeded but no token field was found. Check AuthResponse JSON and update src/App.jsx.");
        }
        localStorage.setItem("psa_token", receivedToken);
        localStorage.setItem("psa_email", authForm.email.trim());
        setToken(receivedToken);
        setUserEmail(authForm.email.trim());
        setNotice("Logged in successfully.");
      }
    } catch (error) {
      setAuthError(error?.message && !error?.response ? error.message : getErrorMessage(error));
    } finally {
      setAuthBusy(false);
    }
  }

  function logout(showMessage = true) {
    localStorage.removeItem("psa_token");
    localStorage.removeItem("psa_email");
    setToken("");
    setUserEmail("");
    setProducts([]);
    setOtp("");
    if (showMessage) setNotice("You have been logged out.");
  }

  function openCreate() {
    setProductForm(emptyProduct);
    setModal({ mode: "create" });
  }

  function openEdit(product) {
    setProductForm({
      name: product.name ?? "",
      description: product.description ?? "",
      price: String(product.price ?? ""),
      stock: String(product.stock ?? ""),
      category: product.category ?? ""
    });
    setModal({ mode: "edit", id: product.id });
  }

  async function saveProduct(event) {
    event.preventDefault();
    setSaving(true);
    setPageError("");
    setNotice("");
    const payload = {
      name: productForm.name.trim(),
      description: productForm.description.trim(),
      price: Number(productForm.price),
      stock: Number(productForm.stock),
      category: productForm.category.trim()
    };
    try {
      if (modal.mode === "create") {
        await api.post("/api/products", payload);
        setNotice("Product created.");
      } else {
        await api.put(`/api/products/${modal.id}`, payload);
        setNotice("Product updated.");
      }
      setModal(null);
      await loadProducts();
    } catch (error) {
      setPageError(getErrorMessage(error));
    } finally {
      setSaving(false);
    }
  }

  async function deleteProduct(product) {
    if (!window.confirm(`Delete "${product.name}"?`)) return;
    setPageError("");
    setNotice("");
    try {
      await api.delete(`/api/products/${product.id}`);
      setNotice("Product deleted.");
      await loadProducts();
    } catch (error) {
      setPageError(getErrorMessage(error));
    }
  }
  async function generateOtp() {
    setOtpBusy(true);
    setPageError("");
    setNotice("");

    try {
      await api.post("/api/auth/generate-otp");
      setShowOtp(true);
      setNotice(
          "OTP generated. In mock mode, check the Spring Boot console for the OTP."
      );
    } catch (error) {
      setPageError(getErrorMessage(error));
    } finally {
      setOtpBusy(false);
    }
  }

  async function verifyEmail(event) {
    event.preventDefault();
    setOtpBusy(true);
    setPageError("");
    setNotice("");

    try {
      await api.post("/api/auth/verify-email", {
        email: userEmail,
        otp: otp.trim(),
      });

      setEmailVerified(true);
      setNotice("Email verified successfully.");
      setOtp("");
      setShowOtp(false);
    } catch (error) {
      setPageError(getErrorMessage(error));
    } finally {
      setOtpBusy(false);
    }
  }
  if (!token) {
    return (
      <main className="auth-shell">
        <section className="auth-aside">
          <div className="brand"><div className="brand-mark"><ShieldCheck size={22} /></div><span>Secure<span className="brand-light">Stock</span></span></div>
          <div className="aside-copy">
            <div className="eyebrow"><span className="pulse" /> PRODUCT SECURE API</div>
            <h1>Your products.<br /><em>Under control.</em></h1>
            <p>A simple management console powered by Spring Boot, JWT authentication, PostgreSQL and Redis.</p>
            <div className="feature-list">
              <div><Database size={17} /><span>Persistent product data</span></div>
              <div><KeyRound size={17} /><span>JWT-secured API access</span></div>
              <div><Activity size={17} /><span>Fast Redis caching</span></div>
            </div>
          </div>
          <div className="aside-foot">BUILT FOR PRODUCTIVITY <span>•</span> API CONSOLE</div>
        </section>
        <section className="auth-main">
          <div className="auth-card">
            <div className="mobile-brand brand"><div className="brand-mark"><ShieldCheck size={22} /></div><span>Secure<span className="brand-light">Stock</span></span></div>
            <div className="form-topline">WELCOME {authMode === "login" ? "BACK" : "ABOARD"}</div>
            <h2>{authMode === "login" ? "Sign in to your workspace" : "Create your account"}</h2>
            <p className="muted">{authMode === "login" ? "Enter your credentials to continue." : "Create an account to start managing products."}</p>
            {notice && <div className="alert success"><Check size={16} />{notice}</div>}
            {authError && <div className="alert error">{authError}</div>}
            <form onSubmit={submitAuth} className="form-stack">
              {authMode === "register" && <label>Full name<input required maxLength="100" autoComplete="name" value={authForm.name} onChange={e => setAuthForm({ ...authForm, name: e.target.value })} placeholder="Your name" /></label>}
              <label>Email address<div className="input-icon"><Mail size={17} /><input required type="email" autoComplete="email" value={authForm.email} onChange={e => setAuthForm({ ...authForm, email: e.target.value })} placeholder="you@example.com" /></div></label>
              <label>Password<div className="input-icon"><KeyRound size={17} /><input required type="password" minLength="1" autoComplete={authMode === "login" ? "current-password" : "new-password"} value={authForm.password} onChange={e => setAuthForm({ ...authForm, password: e.target.value })} placeholder="Enter password" /></div></label>
              <button className="button primary full" disabled={authBusy}>{authBusy ? "Please wait…" : authMode === "login" ? "Sign in" : "Create account"}<ChevronRight size={17} /></button>
            </form>
            <div className="switch-auth">{authMode === "login" ? "New to SecureStock?" : "Already have an account?"}<button className="text-button" onClick={() => { setAuthMode(authMode === "login" ? "register" : "login"); setAuthError(""); setNotice(""); }}> {authMode === "login" ? "Create account" : "Sign in"}</button></div>
            <div className="api-note"><span className="status-dot" /> API target <code>localhost:8080</code></div>
          </div>
        </section>
      </main>
    );
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><div className="brand-mark"><ShieldCheck size={22} /></div><span>Secure<span className="brand-light">Stock</span></span></div>
        <div className="side-label">WORKSPACE</div>
        <div className="nav-item active"><Package size={18} /> Product inventory <span className="nav-count">{products.length}</span></div>
        <div className="side-label side-label-spaced">ACCOUNT</div>
        <div className="account-card"><div className="avatar"><CircleUserRound size={21} /></div><div className="account-text"><strong>{userEmail || "Signed in"}</strong><small>Authenticated session</small></div></div>
        <div className="sidebar-bottom">
          <div className="api-status"><span className="status-dot" /><div><strong>API connection</strong><small>localhost:8080</small></div></div>
          <button className="button ghost full" onClick={() => logout()}><LogOut size={16} /> Sign out</button>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div><div className="breadcrumb">WORKSPACE <ChevronRight size={13} /> INVENTORY</div><h1>Product inventory</h1><p className="muted">Manage your catalog from one place.</p></div>
          <div className="top-actions"><button className="button secondary" onClick={loadProducts} disabled={loading}><RefreshCw size={16} className={loading ? "spin" : ""} /> Refresh</button><button className="button primary" onClick={openCreate}><Plus size={17} /> Add product</button></div>
        </header>

        {notice && <div className="alert success page-alert"><Check size={16} />{notice}<button className="alert-close" onClick={() => setNotice("")}><X size={15} /></button></div>}
        {pageError && <div className="alert error page-alert">{pageError}<button className="alert-close" onClick={() => setPageError("")}><X size={15} /></button></div>}

        <section className="stats-grid">
          <div className="stat-card"><div className="stat-icon blue"><Package size={19} /></div><div><span>Total products</span><strong>{products.length}</strong></div><small>IN CATALOG</small></div>
          <div className="stat-card"><div className="stat-icon green"><Database size={19} /></div><div><span>Total stock units</span><strong>{products.reduce((sum, p) => sum + (Number(p.stock) || 0), 0).toLocaleString()}</strong></div><small>ALL PRODUCTS</small></div>
          <div className="stat-card"><div className="stat-icon violet"><ShieldCheck size={19} /></div><div><span>API security</span><strong>JWT</strong></div><small>BEARER TOKEN</small></div>
        </section>

        <section className="panel">
          <div className="panel-head"><div><h2>All products</h2><p className="muted">Your inventory, synced with the backend.</p></div><div className="search-box"><Search size={17} /><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search products..." /></div></div>
          <div className="table-wrap">
            <table>
              <thead><tr><th>PRODUCT</th><th>CATEGORY</th><th>PRICE</th><th>STOCK</th><th>ID</th><th className="align-right">ACTIONS</th></tr></thead>
              <tbody>
                {loading ? <tr><td colSpan="6" className="empty-state"><RefreshCw className="spin" size={20} /> Loading products…</td></tr>
                : visibleProducts.length === 0 ? <tr><td colSpan="6" className="empty-state"><Box size={24} /><strong>{search ? "No matching products" : "No products found"}</strong><span>{search ? "Try a different search term." : "Add a product to get started."}</span></td></tr>
                : visibleProducts.map(product => <tr key={product.id}>
                  <td><div className="product-cell"><div className="product-icon"><Package size={18} /></div><div><strong>{product.name}</strong><small>{product.description || "No description"}</small></div></div></td>
                  <td><span className="category-pill">{product.category || "Uncategorized"}</span></td>
                  <td className="price-cell">${Number(product.price || 0).toFixed(2)}</td>
                  <td><span className={`stock-pill ${(Number(product.stock) || 0) <= 5 ? "low" : ""}`}>{product.stock ?? 0} units</span></td>
                  <td><code className="id-code">#{product.id}</code></td>
                  <td><div className="row-actions"><button className="icon-button" title="Edit product" onClick={() => openEdit(product)}><Pencil size={16} /></button><button className="icon-button danger" title="Delete product" onClick={() => deleteProduct(product)}><Trash2 size={16} /></button></div></td>
                </tr>)}
              </tbody>
            </table>
          </div>
          <div className="panel-foot"><span>Showing <strong>{visibleProducts.length}</strong> of <strong>{products.length}</strong> products</span><span><span className="status-dot" /> Live API data</span></div>
        </section>


        <section className="verify-panel">
          <div className="verify-icon">
            <Mail size={20} />
          </div>

          <div className="verify-copy">
            <h3>Email verification</h3>
            <p className="muted">
              {emailVerified
                  ? "Your email address has been verified."
                  : "Optional verification for your signed-in account."}
            </p>
          </div>

          {emailVerified ? (
              <span className="verified-badge">
      <Check size={16} />
      Email verified
    </span>
          ) : !showOtp ? (
              <button
                  className="button secondary"
                  onClick={generateOtp}
                  disabled={otpBusy}
              >
                <Mail size={16} />
                {otpBusy ? "Generating…" : "Generate OTP"}
              </button>
          ) : (
              <form className="otp-form" onSubmit={verifyEmail}>
                <input
                    aria-label="Email OTP"
                    inputMode="numeric"
                    maxLength="6"
                    value={otp}
                    onChange={(e) => setOtp(e.target.value.replace(/\D/g, ""))}
                    placeholder="6-digit OTP"
                    required
                />
                <button
                    className="button primary"
                    disabled={otpBusy || otp.length === 0}
                >
                  {otpBusy ? "Verifying…" : "Verify email"}
                </button>
                <button
                    type="button"
                    className="icon-button"
                    onClick={() => {
                      setShowOtp(false);
                      setOtp("");
                    }}
                >
                  <X size={16} />
                </button>
              </form>
          )}
        </section>
        <footer className="footer">SECURESTOCK <span>•</span> PRODUCT SECURE API <span>•</span> BUILT WITH SPRING BOOT</footer>
      </main>

      {modal && <div className="modal-backdrop" onMouseDown={e => { if (e.target === e.currentTarget) setModal(null); }}>
        <section className="modal">
          <div className="modal-head"><div><span className="form-topline">{modal.mode === "create" ? "NEW ENTRY" : "EDIT ENTRY"}</span><h2>{modal.mode === "create" ? "Add product" : "Update product"}</h2></div><button className="icon-button" onClick={() => setModal(null)}><X size={18} /></button></div>
          <form onSubmit={saveProduct} className="form-stack">
            <label>Product name<input required maxLength="150" value={productForm.name} onChange={e => setProductForm({ ...productForm, name: e.target.value })} placeholder="e.g. Wireless headphones" /></label>
            <label>Description<textarea rows="3" value={productForm.description} onChange={e => setProductForm({ ...productForm, description: e.target.value })} placeholder="Short product description" /></label>
            <div className="two-fields"><label>Price<input required type="number" min="0" step="0.01" value={productForm.price} onChange={e => setProductForm({ ...productForm, price: e.target.value })} placeholder="0.00" /></label><label>Stock<input required type="number" min="0" step="1" value={productForm.stock} onChange={e => setProductForm({ ...productForm, stock: e.target.value })} placeholder="0" /></label></div>
            <label>Category<input required maxLength="80" value={productForm.category} onChange={e => setProductForm({ ...productForm, category: e.target.value })} placeholder="e.g. Electronics" /></label>
            <div className="modal-actions"><button type="button" className="button secondary" onClick={() => setModal(null)}>Cancel</button><button className="button primary" disabled={saving}>{saving ? "Saving…" : modal.mode === "create" ? "Create product" : "Save changes"}<ChevronRight size={16} /></button></div>
          </form>
        </section>
      </div>}
    </div>
  );
}

export default App;