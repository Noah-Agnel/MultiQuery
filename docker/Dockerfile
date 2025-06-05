FROM tabulario/spark-iceberg:latest


# Install Apache Livy
USER root


# Download and install Livy
ENV LIVY_VERSION=0.8.0-incubating
RUN wget https://archive.apache.org/dist/incubator/livy/${LIVY_VERSION}/apache-livy-${LIVY_VERSION}_2.12-bin.zip && \
    unzip apache-livy-${LIVY_VERSION}_2.12-bin.zip    && \
    mv apache-livy-${LIVY_VERSION}_2.12-bin /opt/livy && \
    rm apache-livy-${LIVY_VERSION}_2.12-bin.zip


# Set environment variables
ENV LIVY_HOME=/opt/livy
ENV PATH=$PATH:$LIVY_HOME/bin

# Create directories
RUN mkdir -p /opt/livy/logs && \
    mkdir -p /opt/livy/conf

# Copy Iceberg jars to Livy classpath
RUN cp /opt/spark/jars/iceberg-*.jar /opt/livy/jars/ && \
    cp /opt/spark/jars/aws-*.jar /opt/livy/jars/ && \
    cp /opt/spark/jars/hadoop-aws-*.jar /opt/livy/jars/ && \
    cp /opt/spark/jars/postgresql-*.jar /opt/livy/jars/

# Set working directory
WORKDIR /opt/livy

# Expose Livy port
EXPOSE 8998

# Default command
CMD ["bin/livy-server"]