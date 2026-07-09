ARG DOCKER_REGISTRY=docker.io

FROM ${DOCKER_REGISTRY}/node:18-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm config set registry https://mirrors.huaweicloud.com/repository/npm/
RUN npm ci

COPY . .
RUN npm run build

FROM ${DOCKER_REGISTRY}/nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
