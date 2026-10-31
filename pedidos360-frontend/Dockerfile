
FROM node:24-alpine AS build

WORKDIR /app

COPY package*.json ./

RUN npm ci --legacy-peer-deps \
    || (cat /root/.npm/_logs/*-debug-0.log && exit 1)

COPY . .

RUN npm run build -- --configuration development

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist/pedidos360-frontend/browser/ /usr/share/nginx/html/

EXPOSE 80

