-- ==========================================================
-- BASE: CATEGORIES, CUSTOMERS, SUPPLIERS, USERS, PRODUCTS
-- ==========================================================

DO
$$
    BEGIN
        -- CATEGORIES
        IF NOT EXISTS (SELECT 1 FROM public.categories) THEN
            INSERT INTO public.categories (name, description, deleted, created_at, updated_at)
            VALUES ('Bebidas', 'Bebidas gaseosas y jugos', false, now(), now()),
                   ('Snacks', 'Snacks y galletas', false, now(), now()),
                   ('Lácteos', 'Productos lácteos', false, now(), now()),
                   ('Limpieza', 'Productos de limpieza', false, now(), now());
        END IF;

        -- CUSTOMERS
        IF NOT EXISTS (SELECT 1 FROM public.customers) THEN
            INSERT INTO public.customers (full_name, email, deleted, created_at, updated_at)
            VALUES ('Juan Pérez', 'juan.perez@test.com', false, now(), now()),
                   ('María Torres', 'maria.torres@test.com', false, now(), now()),
                   ('Carlos Rojas', 'carlos.rojas@test.com', false, now(), now());
        END IF;

        -- SUPPLIERS
        IF NOT EXISTS (SELECT 1 FROM public.suppliers) THEN
            INSERT INTO public.suppliers (name, document_number, phone, email, deleted, created_at, updated_at)
            VALUES ('Distribuidora Lima SAC', '20123456789', '999111222', 'proveedor@distlima.com', false, now(),
                    now());
        END IF;

        -- USERS
        IF NOT EXISTS (SELECT 1 FROM public.users) THEN
            INSERT INTO public.users (email, password_hash, enabled, created_at)
            VALUES ('admin@local.com', '$2a$10$UinI/Acwt4VWAX5xnjQBPuYWQ4zNCKfL59MD9J5c/OnPUy16RLWei', true, now()),
                   ('seller@local.com', '$2a$12$u8lP7C5GCcu7/53uyD/5au8jAM.jxVwDJGjkicWfHBT1aoE1Kdvgq', true, now()),
                   ('warehouse@local.com', '$2a$12$eAZjqXHgwkN8JMvBaXNSaueRl3shWnehuW3hMz9D.Xul2wW0UZ5HO', true, now());

            -- Roles
            INSERT INTO public.user_roles (user_id, role)
            SELECT u.id, 'ADMIN'
            FROM public.users u
            WHERE u.email = 'admin@local.com'
            UNION ALL
            SELECT u.id, 'SELLER'
            FROM public.users u
            WHERE u.email = 'seller@local.com'
            UNION ALL
            SELECT u.id, 'WAREHOUSE'
            FROM public.users u
            WHERE u.email = 'warehouse@test.com';
        END IF;
    END
$$;

-- PRODUCTS (con FK a category)
DO
$$
    DECLARE
        v_bebidas  BIGINT;
        v_snacks   BIGINT;
        v_lacteos  BIGINT;
        v_limpieza BIGINT;
    BEGIN
        SELECT id INTO v_bebidas FROM public.categories WHERE name = 'Bebidas' LIMIT 1;
        SELECT id INTO v_snacks FROM public.categories WHERE name = 'Snacks' LIMIT 1;
        SELECT id INTO v_lacteos FROM public.categories WHERE name = 'Lácteos' LIMIT 1;
        SELECT id INTO v_limpieza FROM public.categories WHERE name = 'Limpieza' LIMIT 1;

        IF NOT EXISTS (SELECT 1 FROM public.products) THEN
            INSERT INTO public.products (name, description, price, stock, category_id, deleted, created_at, updated_at)
            VALUES ('Coca Cola 500ml', 'Gaseosa personal', 3.50, 0, v_bebidas, false, now(), now()),
                   ('Inka Kola 500ml', 'Gaseosa personal', 3.50, 0, v_bebidas, false, now(), now()),
                   ('Papas Lays 45g', 'Snack clásico', 2.00, 0, v_snacks, false, now(), now()),
                   ('Galletas Oreo', 'Paquete 6u', 4.20, 0, v_snacks, false, now(), now()),
                   ('Leche Gloria 1L', 'Leche entera', 4.80, 0, v_lacteos, false, now(), now()),
                   ('Yogurt fresa', 'Vaso 180g', 2.50, 0, v_lacteos, false, now(), now()),
                   ('Detergente 1kg', 'Multiusos', 8.90, 0, v_limpieza, false, now(), now()),
                   ('Lejía 1L', 'Desinfectante', 4.50, 0, v_limpieza, false, now(), now());
        END IF;
    END
$$;


-- ==========================================================
-- PURCHASES (POSTED) + PURCHASE_ITEMS + PRODUCT_BATCHES + MOVEMENTS(IN)
-- Crea lotes para que el stock provenga de batches (consistente).
-- ==========================================================

DO
$$
    DECLARE
        v_supplier  BIGINT;
        v_admin     BIGINT;
        p_cola      BIGINT;
        p_inka      BIGINT;
        p_lays      BIGINT;
        p_leche     BIGINT;
        v_purchase1 BIGINT;
        v_pi1       BIGINT;
        v_pi2       BIGINT;
        v_pi3       BIGINT;
        v_pi4       BIGINT;
        v_mov       BIGINT;
    BEGIN
        -- Solo si NO existen compras ni batches (seed limpio)
        IF EXISTS (SELECT 1 FROM public.purchases) OR EXISTS (SELECT 1 FROM public.product_batches) THEN
            RETURN;
        END IF;

        SELECT id INTO v_supplier FROM public.suppliers WHERE name = 'Distribuidora Lima SAC' LIMIT 1;
        SELECT id INTO v_admin FROM public.users WHERE email = 'admin@test.com' LIMIT 1;

        SELECT id INTO p_cola FROM public.products WHERE name = 'Coca Cola 500ml' LIMIT 1;
        SELECT id INTO p_inka FROM public.products WHERE name = 'Inka Kola 500ml' LIMIT 1;
        SELECT id INTO p_lays FROM public.products WHERE name = 'Papas Lays 45g' LIMIT 1;
        SELECT id INTO p_leche FROM public.products WHERE name = 'Leche Gloria 1L' LIMIT 1;

        -- =========================
        -- PURCHASE #1 (POSTED)
        -- =========================
        INSERT INTO public.purchases
        (purchase_date, status, document_type, document_number, notes, supplier_id, total_amount,
         created_by_user_id, posted_at, posted_by_user_id, created_at, updated_at)
        VALUES (now() - interval '5 days', 'POSTED', 'INVOICE', 'P-0001', 'Compra seed #1', v_supplier, 0,
                v_admin, now() - interval '5 days', v_admin, now(), now())
        RETURNING id INTO v_purchase1;

        -- Items (cantidades grandes para que luego puedas vender y testear FEFO)
        INSERT INTO public.purchase_items (purchase_id, product_id, quantity, unit_cost, sub_total)
        VALUES (v_purchase1, p_cola, 40, 2.10, 84.00),
               (v_purchase1, p_inka, 30, 2.10, 63.00),
               (v_purchase1, p_lays, 80, 1.10, 88.00),
               (v_purchase1, p_leche, 25, 3.20, 80.00);

        -- Obtener IDs de purchase_items
        SELECT id INTO v_pi1 FROM public.purchase_items WHERE purchase_id = v_purchase1 AND product_id = p_cola LIMIT 1;
        SELECT id INTO v_pi2 FROM public.purchase_items WHERE purchase_id = v_purchase1 AND product_id = p_inka LIMIT 1;
        SELECT id INTO v_pi3 FROM public.purchase_items WHERE purchase_id = v_purchase1 AND product_id = p_lays LIMIT 1;
        SELECT id
        INTO v_pi4
        FROM public.purchase_items
        WHERE purchase_id = v_purchase1 AND product_id = p_leche
        LIMIT 1;

        -- Batches (FEFO: leche perecible, bebidas/snacks también si quieres probar expiración)
        INSERT INTO public.product_batches
        (product_id, purchase_item_id, batch_code, received_at, expires_at, qty_initial, qty_available, unit_cost,
         created_at, updated_at)
        VALUES
            -- Coca (2 lotes)
            (p_cola, v_pi1, 'COLA-A', now() - interval '5 days', now() + interval '90 days', 20, 20, 2.10, now(),
             now()),
            (p_cola, v_pi1, 'COLA-B', now() - interval '5 days', now() + interval '120 days', 20, 20, 2.10, now(),
             now()),

            -- Inka (2 lotes)
            (p_inka, v_pi2, 'INKA-A', now() - interval '5 days', now() + interval '80 days', 15, 15, 2.10, now(),
             now()),
            (p_inka, v_pi2, 'INKA-B', now() - interval '5 days', now() + interval '110 days', 15, 15, 2.10, now(),
             now()),

            -- Lays (1 lote)
            (p_lays, v_pi3, 'LAYS-A', now() - interval '5 days', now() + interval '180 days', 80, 80, 1.10, now(),
             now()),

            -- Leche (2 lotes con expiración distinta para FEFO)
            (p_leche, v_pi4, 'LECHE-01', now() - interval '5 days', now() + interval '10 days', 10, 10, 3.20, now(),
             now()),
            (p_leche, v_pi4, 'LECHE-02', now() - interval '5 days', now() + interval '25 days', 15, 15, 3.20, now(),
             now());

        -- Recalcular stock = SUM(qty_available) por producto (consistente con batches)
        UPDATE public.products p
        SET stock      = COALESCE(t.sum_avail, 0),
            updated_at = now()
        FROM (SELECT product_id, SUM(qty_available) AS sum_avail
              FROM public.product_batches
              GROUP BY product_id) t
        WHERE p.id = t.product_id;

        -- Movement IN (opcional pero recomendado si tu app lo usa)
        INSERT INTO public.inventory_movements
        (movement_type, source_type, source_id, reason, created_by_user_id, created_at, updated_at, event_type)
        VALUES ('IN', 'PURCHASE', v_purchase1, 'Compra seed #1', v_admin, now(), now(), 'PURCHASE_IN')
        RETURNING id INTO v_mov;

        INSERT INTO public.inventory_movement_items
            (movement_id, product_id, quantity, previous_stock, new_stock)
        VALUES (v_mov, p_cola, 40, 0, 40),
               (v_mov, p_inka, 30, 0, 30),
               (v_mov, p_lays, 80, 0, 80),
               (v_mov, p_leche, 25, 0, 25);

        -- Total_amount (si quieres mantenerlo correcto)
        UPDATE public.purchases
        SET total_amount = 84.00 + 63.00 + 88.00 + 80.00,
            updated_at   = now()
        WHERE id = v_purchase1;
    END
$$;


-- ==========================================================
-- SALES (ACTIVE) + DETAILS + ALLOCATIONS + MOVEMENTS(OUT)
-- Consume batches FEFO y baja stock/qty_available consistentemente.
-- ==========================================================

DO
$$
    DECLARE
        v_customer1 BIGINT;
        v_customer2 BIGINT;
        v_seller    BIGINT;
        p_cola      BIGINT;
        p_lays      BIGINT;
        p_leche     BIGINT;

        -- batches
        b_cola_a    BIGINT;
        b_cola_b    BIGINT;
        b_lays_a    BIGINT;
        b_leche_01  BIGINT;
        b_leche_02  BIGINT;
        v_sale1     BIGINT;
        v_sale2     BIGINT;
        d1          BIGINT;
        d2          BIGINT;
        d3          BIGINT;
        v_mov1      BIGINT;
        v_mov2      BIGINT;
        prev_cola   INT;
        prev_lays   INT;
        prev_leche  INT;
    BEGIN
        IF EXISTS (SELECT 1 FROM public.sales) THEN
            RETURN;
        END IF;

        SELECT id INTO v_customer1 FROM public.customers WHERE email = 'juan.perez@test.com' LIMIT 1;
        SELECT id INTO v_customer2 FROM public.customers WHERE email = 'maria.torres@test.com' LIMIT 1;
        SELECT id INTO v_seller FROM public.users WHERE email = 'seller@test.com' LIMIT 1;

        SELECT id INTO p_cola FROM public.products WHERE name = 'Coca Cola 500ml' LIMIT 1;
        SELECT id INTO p_lays FROM public.products WHERE name = 'Papas Lays 45g' LIMIT 1;
        SELECT id INTO p_leche FROM public.products WHERE name = 'Leche Gloria 1L' LIMIT 1;

        -- Batch IDs
        SELECT id INTO b_cola_a FROM public.product_batches WHERE batch_code = 'COLA-A' LIMIT 1;
        SELECT id INTO b_cola_b FROM public.product_batches WHERE batch_code = 'COLA-B' LIMIT 1;
        SELECT id INTO b_lays_a FROM public.product_batches WHERE batch_code = 'LAYS-A' LIMIT 1;
        SELECT id INTO b_leche_01 FROM public.product_batches WHERE batch_code = 'LECHE-01' LIMIT 1;
        SELECT id INTO b_leche_02 FROM public.product_batches WHERE batch_code = 'LECHE-02' LIMIT 1;

        -- =========================
        -- SALE #1 (ACTIVE): 3 colas + 2 lays
        -- cola: tomar primero COLA-A (FEFO no aplica mucho, pero igual)
        -- =========================
        SELECT stock INTO prev_cola FROM public.products WHERE id = p_cola LIMIT 1;
        SELECT stock INTO prev_lays FROM public.products WHERE id = p_lays LIMIT 1;

        INSERT INTO public.sales
        (sale_date, status, total_amount, customer_id, created_by_user_id, posted_at, posted_by_user_id, created_at,
         updated_at)
        VALUES (now() - interval '2 days', 'ACTIVE', (3 * 3.50 + 2 * 2.00), v_customer1, v_seller,
                now() - interval '2 days', v_seller, now(), now())
        RETURNING id INTO v_sale1;

        -- Insert sale_details y obtener IDs de forma separada
        INSERT INTO public.sale_details
            (sale_id, product_id, quantity, unit_price, sub_total)
        VALUES (v_sale1, p_cola, 3, 3.50, 10.50);

        SELECT id INTO d1 FROM public.sale_details WHERE sale_id = v_sale1 AND product_id = p_cola LIMIT 1;

        INSERT INTO public.sale_details
            (sale_id, product_id, quantity, unit_price, sub_total)
        VALUES (v_sale1, p_lays, 2, 2.00, 4.00);

        SELECT id INTO d2 FROM public.sale_details WHERE sale_id = v_sale1 AND product_id = p_lays LIMIT 1;

        -- Allocations (cola 3 de COLA-A, lays 2 de LAYS-A)
        INSERT INTO public.sale_batch_allocations (sale_detail_id, product_batch_id, quantity)
        VALUES (d1, b_cola_a, 3),
               (d2, b_lays_a, 2);

        -- Baja qty_available batches
        UPDATE public.product_batches SET qty_available = qty_available - 3, updated_at = now() WHERE id = b_cola_a;
        UPDATE public.product_batches SET qty_available = qty_available - 2, updated_at = now() WHERE id = b_lays_a;

        -- Baja stock agregado
        UPDATE public.products SET stock = stock - 3, updated_at = now() WHERE id = p_cola;
        UPDATE public.products SET stock = stock - 2, updated_at = now() WHERE id = p_lays;

        -- Movement OUT
        INSERT INTO public.inventory_movements
        (movement_type, source_type, source_id, reason, created_by_user_id, created_at, updated_at, event_type)
        VALUES ('OUT', 'SALE', v_sale1, 'Venta seed #1', v_seller, now(), now(), 'SALE_OUT')
        RETURNING id INTO v_mov1;

        INSERT INTO public.inventory_movement_items
            (movement_id, product_id, quantity, previous_stock, new_stock)
        VALUES (v_mov1, p_cola, 3, prev_cola, prev_cola - 3),
               (v_mov1, p_lays, 2, prev_lays, prev_lays - 2);


        -- =========================
        -- SALE #2 (ACTIVE): 12 leches (debe consumir FEFO: primero LECHE-01 (10), luego LECHE-02 (2))
        -- =========================
        SELECT stock INTO prev_leche FROM public.products WHERE id = p_leche LIMIT 1;

        INSERT INTO public.sales
        (sale_date, status, total_amount, customer_id, created_by_user_id, posted_at, posted_by_user_id, created_at,
         updated_at)
        VALUES (now() - interval '1 days', 'ACTIVE', (12 * 4.80), v_customer2, v_seller, now() - interval '1 days',
                v_seller, now(), now())
        RETURNING id INTO v_sale2;

        INSERT INTO public.sale_details
            (sale_id, product_id, quantity, unit_price, sub_total)
        VALUES (v_sale2, p_leche, 12, 4.80, 57.60);

        SELECT id INTO d3 FROM public.sale_details WHERE sale_id = v_sale2 AND product_id = p_leche LIMIT 1;

        -- allocations FEFO
        INSERT INTO public.sale_batch_allocations (sale_detail_id, product_batch_id, quantity)
        VALUES (d3, b_leche_01, 10),
               (d3, b_leche_02, 2);

        UPDATE public.product_batches SET qty_available = qty_available - 10, updated_at = now() WHERE id = b_leche_01;
        UPDATE public.product_batches SET qty_available = qty_available - 2, updated_at = now() WHERE id = b_leche_02;

        UPDATE public.products SET stock = stock - 12, updated_at = now() WHERE id = p_leche;

        INSERT INTO public.inventory_movements
        (movement_type, source_type, source_id, reason, created_by_user_id, created_at, updated_at, event_type)
        VALUES ('OUT', 'SALE', v_sale2, 'Venta seed #2 (FEFO leche)', v_seller, now(), now(), 'SALE_OUT')
        RETURNING id INTO v_mov2;

        INSERT INTO public.inventory_movement_items
            (movement_id, product_id, quantity, previous_stock, new_stock)
        VALUES (v_mov2, p_leche, 12, prev_leche, prev_leche - 12);

    END
$$;