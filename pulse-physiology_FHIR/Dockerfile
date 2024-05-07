FROM kitware/pulse:4.1.0

################################################################################
# install some python packages
COPY requirements.txt /tmp/
RUN pip3 install --requirement /tmp/requirements.txt
# RUN jupyter nbextension enable --py bqplot
RUN pip install pulse
RUN pip install fhirclient
RUN pip install urllib3
RUN pip install requests
RUN pip3 install jupyter nbconvert

#added new extensions for deployment in jupyter and
RUN pip install nbconvert

################################################################################
# set up user
ARG NB_USER=notebooks
ARG NB_UID=1000
ENV USER ${NB_USER}
ENV NB_UID ${NB_UID}
ENV HOME /home/${NB_USER}

ENV JUPYTER_ENABLE_LAB=0 
EXPOSE 8888


RUN adduser --disabled-password \
    --gecos "Default user" \
    --uid ${NB_UID} \
    ${NB_USER}

WORKDIR ${HOME}



# Copy our notebooks to ${HOME}
COPY *.ipynb ${HOME}/
COPY *.py ${HOME}/
# Copy pulse data files to ${HOME}/data
RUN mkdir ${HOME}/data
RUN mv /pulse/bin/config/ ${HOME}/data/
RUN mv /pulse/bin/ecg/ ${HOME}/data/
RUN mv /pulse/bin/environments/ ${HOME}/data/
RUN mv /pulse/bin/nutrition/ ${HOME}/data/
RUN mv /pulse/bin/patients/ ${HOME}/data/
RUN mv /pulse/bin/states/ ${HOME}/data/
RUN mv /pulse/bin/substances/ ${HOME}/data/
RUN mv /pulse/bin/*.json ${HOME}/data
RUN mv /pulse/bin/PyPulse* ${HOME}
# Copy pulse python files to ${HOME}
RUN mv /pulse/python/ ${HOME}/python/
ENV PYTHONPATH ${HOME}:${HOME}/python
# Change ownership to the binder runner
USER root
RUN chown -R ${NB_UID} ${HOME}
USER ${NB_USER}
# add new command to jupyter notebook to run python script in container with nbconvert command line arguments
# CMD ["jupyter", "nbconvert", "--to", "python", ${HOME}"/ppe_fhir.ipynb", "&&", "python", ${HOME}"/ppe_fhir.py"]
CMD ["jupyter", "nbconvert", "--to", "python", "${HOME}/ppe_fhir.ipynb"  && \
     python ${HOME}/ppe_fhir.py || \
     echo 'Notebook conversion and/or execution failed!'] 

