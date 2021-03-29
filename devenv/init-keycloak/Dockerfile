FROM python:3.7-alpine
WORKDIR /scripts
RUN apk update \
 && apk add --no-cache \
        gcc \
        python3-dev \
        musl-dev \
        linux-headers \
        bash \
        curl
COPY requirements.txt requirements.txt
RUN pip install -r requirements.txt
COPY . .
RUN chmod +x ./init-keycloak.py
CMD ["init-keycloak.py"]
ENTRYPOINT ["python3"]
