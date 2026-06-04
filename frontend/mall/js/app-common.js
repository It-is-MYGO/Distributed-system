const AppCommon = (() => {
    const API_BASE = "/api";
    const TOKEN_KEY = "demo.jwt_token";
    const CART_CACHE_KEY = "demo.cart_cache";
    const FALLBACK_IMAGES = [
        "mall/image/sub_banner/r1.jpg",
        "mall/image/sub_banner/r2.jpg",
        "mall/image/sub_banner/hot1.png",
        "mall/image/sub_banner/pc.jpg",
        "mall/image/sub_banner/headphones.jpg"
    ];

    const $ = (selector, root = document) => root.querySelector(selector);
    const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));

    function escapeHtml(value) {
        return String(value ?? "").replace(/[&<>"']/g, (char) => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        }[char]));
    }

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function setToken(token) {
        localStorage.setItem(TOKEN_KEY, token);
    }

    function removeToken() {
        localStorage.removeItem(TOKEN_KEY);
    }

    let afterAuthAction = null;

    function requireLogin(options = {}) {
        if (!getToken()) {
            if (options.redirect) {
                location.href = `/index.html?redirect=${encodeURIComponent(location.pathname + location.search)}`;
            } else {
                showAuthModal(options.afterLogin);
            }
            return false;
        }
        return true;
    }

    async function apiRequest(endpoint, options = {}) {
        const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
        const token = getToken();
        if (token) headers.Authorization = `Bearer ${token}`;

        const response = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });
        if (response.status === 401) {
            removeToken();
            requireLogin();
            return null;
        }
        if (!response.ok) {
            const text = await response.text();
            let message = response.statusText;
            try {
                const json = JSON.parse(text);
                message = json.message || json.error || message;
            } catch {
                message = text || message;
            }
            throw new Error(message);
        }
        if (response.status === 204) return null;
        const text = await response.text();
        if (!text || !text.trim()) return null;
        try {
            return JSON.parse(text);
        } catch {
            return text;
        }
    }

    function formatMoney(value) {
        return `¥${Number(value || 0).toFixed(2)}`;
    }

    function formatDate(value) {
        if (!value) return "-";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;
        return date.toLocaleString("zh-CN", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function productImage(product, index = 0) {
        if (product?.coverImage) return product.coverImage;
        return FALLBACK_IMAGES[index % FALLBACK_IMAGES.length];
    }

    function avatarFor(source = {}, fallback = "user") {
        const username = source.username || fallback;
        if (username) {
            try {
                const localAvatar = localStorage.getItem(`demo.local_avatar:${username}`);
                if (localAvatar) return localAvatar;
            } catch {
            }
        }
        const direct = source.avatarUrl || source.avatar || source.headImage;
        if (direct) return direct;
        const seed = encodeURIComponent(source.username || source.nickName || fallback || "user");
        return `https://api.dicebear.com/8.x/thumbs/svg?seed=${seed}`;
    }

    function mapStatus(status) {
        const value = String(status || "").toUpperCase();
        return {
            CREATED: "待支付",
            PAID: "已支付",
            SHIPPED: "已发货",
            DELIVERED: "已到货",
            COMPLETED: "已完成",
            CANCELLED: "已取消"
        }[value] || status || "未知";
    }

    function toast(message, type = "info") {
        const node = document.createElement("div");
        node.className = `toast toast-${type}`;
        node.innerHTML = `<span class="toast-dot"></span><strong>${escapeHtml(message)}</strong>`;
        document.body.appendChild(node);
        requestAnimationFrame(() => node.classList.add("show"));
        setTimeout(() => {
            node.classList.remove("show");
            setTimeout(() => node.remove(), 220);
        }, 2200);
    }

    function showAuthModal(afterLogin) {
        afterAuthAction = typeof afterLogin === "function" ? afterLogin : null;
        let modal = $("#authModal");
        if (!modal) {
            modal = document.createElement("div");
            modal.id = "authModal";
            modal.className = "auth-modal";
            const redirect = encodeURIComponent(location.pathname + location.search);
            modal.innerHTML = `
                <div class="auth-dialog">
                    <button class="auth-close" id="authClose" type="button">×</button>
                    <h2 style="margin:0 0 8px;">请先登录</h2>
                    <p style="color:var(--muted);margin:0 0 18px;">登录后即可加入购物车、立即购买和查看订单。</p>
                    <form class="auth-form" id="quickLoginForm">
                        <div class="field"><label for="quickLoginName">账号</label><input id="quickLoginName" autocomplete="username" placeholder="请输入账号"></div>
                        <div class="field"><label for="quickLoginPassword">密码</label><input id="quickLoginPassword" type="password" autocomplete="current-password" placeholder="请输入密码"></div>
                        <button class="btn primary" type="submit">登录并继续</button>
                    </form>
                    <div class="auth-footer">
                        <span>没有账号？</span>
                        <a class="btn ghost" href="/register.html?redirect=${redirect}">去注册</a>
                    </div>
                </div>
            `;
            document.body.appendChild(modal);
            bindAuthModal(modal);
        }
        modal.classList.add("show");
    }

    function hideAuthModal() {
        $("#authModal")?.classList.remove("show");
    }

    function bindAuthModal(modal) {
        $("#authClose", modal).addEventListener("click", hideAuthModal);
        modal.addEventListener("click", event => {
            if (event.target === modal) hideAuthModal();
        });
        $("#quickLoginForm", modal).addEventListener("submit", async event => {
            event.preventDefault();
            const username = $("#quickLoginName", modal).value.trim();
            const password = $("#quickLoginPassword", modal).value.trim();
            if (!username || !password) return toast("请输入账号和密码", "error");
            try {
                const result = await apiRequest("/user/login", {
                    method: "POST",
                    body: JSON.stringify({ username, password })
                });
                if (!result?.token) throw new Error(result?.message || "登录失败");
                setToken(result.token);
                hideAuthModal();
                await renderHeader(document.body.dataset.active || "shop");
                toast("登录成功", "success");
                if (afterAuthAction) {
                    const action = afterAuthAction;
                    afterAuthAction = null;
                    await action();
                }
            } catch (error) {
                toast(`登录失败：${error.message}`, "error");
            }
        });
    }

    function loading(label = "加载中...") {
        return `<div class="empty-state"><div class="loader"></div><p>${escapeHtml(label)}</p></div>`;
    }

    function emptyState(title, text = "", action = "") {
        return `
            <div class="empty-state">
                <h3>${escapeHtml(title)}</h3>
                ${text ? `<p>${escapeHtml(text)}</p>` : ""}
                ${action}
            </div>
        `;
    }

    async function getCartCount() {
        if (!getToken()) return 0;
        try {
            const items = await apiRequest("/cart");
            return (items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0);
        } catch {
            return 0;
        }
    }

    async function loadCurrentUser() {
        if (!getToken()) return null;
        try {
            const user = await apiRequest("/user/me");
            if (user?.username) {
                try {
                    const localAvatar = localStorage.getItem(`demo.local_avatar:${user.username}`);
                    if (localAvatar) user.avatarUrl = localAvatar;
                } catch {
                }
            }
            return user;
        } catch {
            return null;
        }
    }

    function readMessages() {
        try {
            const saved = JSON.parse(localStorage.getItem("demo.message_box") || "[]");
            return Array.isArray(saved) ? saved : [];
        } catch {
            return [];
        }
    }

    async function renderHeader(active = "shop") {
        const host = $("#siteHeader");
        if (!host) return;
        const user = await loadCurrentUser();
        const count = await getCartCount();
        const messages = user ? readMessages() : [];
        const unreadCount = messages.filter(message => !message.read).length;
        const latestMessages = messages.slice(0, 1);
        const keyword = new URLSearchParams(location.search).get("keyword") || "";
        host.innerHTML = `
            <div class="site-bar">
                <a class="brand" href="/shop.html" aria-label="分布式秒杀系统首页">
                    <span class="brand-mark">F</span>
                    <span><strong>分布式秒杀系统</strong><small>商品库存与秒杀商城</small></span>
                </a>
                <form class="global-search" id="globalSearch">
                    <input id="globalKeyword" value="${escapeHtml(keyword)}" placeholder="搜索手机、家电、生活好物">
                    <button type="submit">搜索</button>
                </form>
                <nav class="site-nav">
                    ${user?.role === "ADMIN" ? `
                        <a class="${active === "admin" ? "active" : ""}" href="/product.html">商品管理</a>
                    ` : `
                        <a class="${active === "shop" ? "active" : ""}" href="/shop.html">首页</a>
                        <a class="${active === "search" ? "active" : ""}" href="/search.html">分类</a>
                        <a class="${active === "cart" ? "active" : ""}" href="/cart.html">购物车<span class="count">${count}</span></a>
                        <a class="${active === "orders" ? "active" : ""}" href="/orders.html">订单</a>
                        <span class="nav-message-wrap">
                            <a class="nav-bell ${active === "messages" ? "active" : ""}" href="/messages.html" aria-label="消息邮箱" title="消息邮箱">
                                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></svg>
                                ${unreadCount ? `<span class="count">${unreadCount}</span>` : ""}
                            </a>
                            <span class="message-popover">
                                <strong>最新一条消息</strong>
                                ${latestMessages.length ? latestMessages.map(message => `
                                    <span class="message-popover-item">
                                        <b>${escapeHtml(message.title)}</b>
                                        <small>${escapeHtml(message.content)}</small>
                                    </span>
                                `).join("") : `<span class="message-popover-empty">暂无新消息</span>`}
                            </span>
                        </span>
                    `}
                    ${user ? `<a class="nav-user ${active === "personal" ? "active" : ""}" href="/personal.html"><img class="avatar avatar-sm" src="${escapeHtml(avatarFor(user))}" alt="${escapeHtml(user.nickName || user.username || "头像")}"><span>${escapeHtml(user.nickName || user.username)}</span></a><button class="link-button" id="logoutBtn">退出</button>` : `<a href="/index.html">登录</a><a href="/register.html">注册</a>`}
                </nav>
            </div>
        `;
        $("#globalSearch")?.addEventListener("submit", (event) => {
            event.preventDefault();
            const value = $("#globalKeyword").value.trim();
            location.href = `/search.html${value ? `?keyword=${encodeURIComponent(value)}` : ""}`;
        });
        $("#logoutBtn")?.addEventListener("click", () => {
            removeToken();
            location.href = "/shop.html";
        });
    }

    function productCard(product, index = 0) {
        const stock = Number(product.stock ?? 0);
        const disabled = stock <= 0 ? "disabled" : "";
        const sales = Number(product.salesCount || 0);
        const rank = Number(product.categorySalesRank || 0);
        const smartTag = stock <= 20 ? "即将售罄" : sales >= 2600 ? "热卖" : rank && rank <= 2 ? "榜单好物" : "精选";
        return `
            <article class="product-card">
                <a class="product-media" href="/detail.html?productId=${product.id}">
                    <img src="${escapeHtml(productImage(product, index))}" alt="${escapeHtml(product.name || "商品图片")}">
                    <span class="tag">${escapeHtml(smartTag)}</span>
                </a>
                <div class="product-body">
                    <a class="product-name" href="/detail.html?productId=${product.id}">${escapeHtml(product.name || "未命名商品")}</a>
                    <div class="product-meta">
                        <strong>${formatMoney(product.price)}</strong>
                        <button class="add-plus js-add-cart" type="button" data-id="${product.id}" ${disabled} aria-label="加入购物车">+</button>
                    </div>
                    <span class="stock-note">${sales ? `已售 ${sales} 件` : escapeHtml(product.couponText || "精选好物")}</span>
                </div>
            </article>
        `;
    }

    const addCartQueue = new Map();

    function updateLocalCartCount(delta) {
        const countNode = document.querySelector(".site-nav a[href='/cart.html'] .count");
        if (!countNode) return;
        const current = Number(countNode.textContent || 0);
        countNode.textContent = Math.max(0, current + delta);
    }

    function mergeLocalCart(productId, quantity) {
        try {
            const saved = JSON.parse(localStorage.getItem(CART_CACHE_KEY) || "[]");
            if (!Array.isArray(saved)) return;
            const item = saved.find(entry => Number(entry.productId) === Number(productId));
            if (item) {
                item.quantity = Number(item.quantity || 0) + Number(quantity || 1);
                localStorage.setItem(CART_CACHE_KEY, JSON.stringify(saved));
            }
        } catch {
        }
    }

    function bindAddToCart(root = document) {
        $$(".js-add-cart", root).forEach((button) => {
            if (button.dataset.cartBound === "1") return;
            button.dataset.cartBound = "1";
            button.addEventListener("click", async () => {
                if (!requireLogin({ afterLogin: () => button.click() })) return;
                queueAddToCart(button);
            });
        });
    }

    async function queueAddToCart(button) {
        const productId = Number(button.dataset.id);
        const key = String(productId);
        const state = addCartQueue.get(key) || { pending: 0, running: false };
        state.pending += 1;
        addCartQueue.set(key, state);
        updateLocalCartCount(1);
        mergeLocalCart(productId, 1);
        animateAddToCart(button);
        toast("已加入购物车", "cart");
        if (state.running) return;
        state.running = true;
        button.classList.add("is-adding");
        try {
            while (state.pending > 0) {
                const quantity = state.pending;
                state.pending = 0;
                try {
                    await apiRequest("/cart", {
                        method: "POST",
                        body: JSON.stringify({ productId, quantity })
                    });
                } catch (error) {
                    toast(`添加失败：${error.message}`, "error");
                    break;
                }
            }
            await renderHeader(document.body.dataset.active || "shop");
        } finally {
            state.running = false;
            button.classList.remove("is-adding");
        }
    }

    function animateAddToCart(button) {
        const rect = button.getBoundingClientRect();
        const cart = document.querySelector(".site-nav a[href='/cart.html']");
        const target = cart?.getBoundingClientRect();
        const fly = document.createElement("span");
        fly.className = "cart-fly";
        fly.textContent = "";
        fly.style.left = `${rect.left + rect.width / 2}px`;
        fly.style.top = `${rect.top + rect.height / 2}px`;
        document.body.appendChild(fly);
        requestAnimationFrame(() => {
            fly.style.transform = target
                ? `translate(${target.left - rect.left}px, ${target.top - rect.top}px) scale(.35)`
                : "translateY(-36px) scale(.5)";
            fly.style.opacity = "0";
        });
        setTimeout(() => fly.remove(), 720);
    }

    function lockBackNavigation() {
        if (window.__mallBackLocked) return;
        window.__mallBackLocked = true;
        history.replaceState({ mallLockedBase: true }, "", location.href);
        history.pushState({ mallLocked: true }, "", location.href);
        window.addEventListener("popstate", () => {
            setTimeout(() => history.forward(), 0);
        });
    }

    setTimeout(lockBackNavigation, 0);

    return {
        $, $$, API_BASE, TOKEN_KEY, escapeHtml, getToken, setToken, removeToken, requireLogin,
        apiRequest, formatMoney, formatDate, productImage, avatarFor, mapStatus, toast, loading,
        emptyState, showAuthModal, hideAuthModal, loadCurrentUser, renderHeader, productCard, bindAddToCart, animateAddToCart, lockBackNavigation
    };
})();

