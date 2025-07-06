DROP DATABASE vcsystems_db; --es mejor limpiarla 

CREATE DATABASE IF NOT EXISTS vcsystems_db;
USE vcsystems_db;


CREATE TABLE usuario (
  id_usuario BIGINT AUTO_INCREMENT PRIMARY KEY,
  rol ENUM('GERENTE', 'CLIENTE', 'TECNICO', 'ADMIN') NOT NULL,
  nombre VARCHAR(255) NOT NULL,
  correo VARCHAR(255) NOT NULL UNIQUE,
  contrasena VARCHAR(255) NOT NULL,
  creado_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);


CREATE TABLE cliente (
  id_cliente BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_usuario BIGINT NOT NULL,
  nombre_empresa VARCHAR(255) NOT NULL,
  direccion_empresa VARCHAR(255),
  creado_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
);


CREATE TABLE proveedor (
  id_proveedor BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(255) NOT NULL,
  telefono VARCHAR(255),
  contacto VARCHAR(255),
  direccion VARCHAR(255),
  creado_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);


CREATE TABLE diccionario_fallas (
  id_falla BIGINT AUTO_INCREMENT PRIMARY KEY,
  codigo_falla VARCHAR(255) NOT NULL UNIQUE,
  descripcion VARCHAR(255),
  creado_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);


CREATE TABLE incidencia (
  id_incidencia BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_cliente BIGINT NOT NULL,
  id_tecnico BIGINT,
  id_falla BIGINT,
  descripcion VARCHAR(255) NOT NULL,
  estado ENUM('PENDIENTE', 'ASIGNADA', 'EN_PROCESO', 'RESUELTA', 'CERRADA') NOT NULL,
  prioridad ENUM('BAJA', 'MEDIA', 'ALTA') DEFAULT 'MEDIA',
  fecha_creacion DATETIME(6),
  fecha_asignacion DATETIME(6),
  fecha_resolucion DATETIME(6),
  creado_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
  FOREIGN KEY (id_tecnico) REFERENCES usuario(id_usuario),
  FOREIGN KEY (id_falla) REFERENCES diccionario_fallas(id_falla)
);


CREATE TABLE solicitud_repuesto (
  id_solicitud BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_incidencia BIGINT NOT NULL,
  id_proveedor BIGINT,
  id_tecnico BIGINT NOT NULL,
  justificacion VARCHAR(255) NOT NULL,
  estado ENUM('PENDIENTE', 'ENVIADO', 'RECIBIDO') NOT NULL DEFAULT 'PENDIENTE',
  creado_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  FOREIGN KEY (id_incidencia) REFERENCES incidencia(id_incidencia),
  FOREIGN KEY (id_proveedor) REFERENCES proveedor(id_proveedor),
  FOREIGN KEY (id_tecnico) REFERENCES usuario(id_usuario)
);


INSERT INTO usuario (rol, nombre, correo, contrasena, creado_at, actualizado_at) VALUES
('GERENTE', 'Gerente General', 'gerente@vcsystems.com', 'password123', NOW(6), NOW(6)),
('TECNICO', 'Técnico Soporte', 'tecnico@vcsystems.com', 'password123', NOW(6), NOW(6)),
('CLIENTE', 'Cliente Demo', 'cliente@empresa.com', 'password123', NOW(6), NOW(6));


INSERT INTO cliente (id_usuario, nombre_empresa, direccion_empresa, creado_at, actualizado_at) VALUES
(3, 'Empresa Demo S.A.C.', 'Av. Principal 123, Lima', NOW(6), NOW(6));


INSERT INTO proveedor (nombre, telefono, contacto, direccion, creado_at, actualizado_at) VALUES
('Repuestos SAC', '+51987654321', 'contacto@repuestos.com', 'Av. Industrial 456, Lima', NOW(6), NOW(6)),
('Soluciones Industriales', '+51912345678', 'ventas@soluciones.com', 'Jr. Comercio 789, Lima', NOW(6), NOW(6));


INSERT INTO diccionario_fallas (codigo_falla, descripcion, creado_at, actualizado_at) VALUES
('F001', 'Fallo en el compresor', NOW(6), NOW(6)),
('F002', 'Fuga de refrigerante', NOW(6), NOW(6)),
('F003', 'Problema eléctrico', NOW(6), NOW(6)),
('F004', 'Sobrecalentamiento', NOW(6), NOW(6)),
('F005', 'Ruido anormal', NOW(6), NOW(6));


INSERT INTO incidencia (id_cliente, id_tecnico, id_falla, descripcion, estado, prioridad, fecha_creacion, creado_at, actualizado_at) VALUES
(1, NULL, 1, 'Compresor no enciende al encender el equipo', 'PENDIENTE', 'ALTA', '2025-07-01 08:00:00', NOW(6), NOW(6)),
(1, 2, 2, 'Se detectó fuga de refrigerante en la unidad exterior', 'ASIGNADA', 'MEDIA', '2025-07-01 10:30:00', NOW(6), NOW(6)),
(1, 2, 3, 'Corto circuito en el panel de control', 'EN_PROCESO', 'ALTA', '2025-07-01 14:15:00', NOW(6), NOW(6)),
(1, NULL, 5, 'Ruido anormal en el equipo durante funcionamiento', 'PENDIENTE', 'BAJA', '2025-07-02 09:20:00', NOW(6), NOW(6));


INSERT INTO solicitud_repuesto (id_incidencia, id_proveedor, id_tecnico, justificacion, estado, creado_at, actualizado_at) VALUES
(2, 1, 2, 'Se requiere válvula de expansión para reparar fuga', 'PENDIENTE', NOW(6), NOW(6)),
(3, 2, 2, 'Necesario reemplazar panel de control dañado', 'ENVIADO', NOW(6), NOW(6));

-- pruebitas
SHOW TABLES;
SELECT 'USUARIOS:' as tabla;
SELECT id_usuario, rol, nombre, correo FROM usuario;
SELECT 'CLIENTES:' as tabla;
SELECT * FROM cliente;
SELECT 'INCIDENCIAS:' as tabla;
SELECT id_incidencia, descripcion, estado, prioridad FROM incidencia;

SELECT correo, contrasena FROM usuario;
SELECT * FROM usuario;
